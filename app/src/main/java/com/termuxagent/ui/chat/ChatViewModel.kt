package com.termuxagent.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termuxagent.data.agent.Agent
import com.termuxagent.data.agent.AgentEvent
import com.termuxagent.data.agent.ToolRegistry
import com.termuxagent.data.agent.tools.ToolResult
import com.termuxagent.data.api.ChatMessage
import com.termuxagent.data.api.OpenAIClient
import com.termuxagent.data.chat.AssistantBlock
import com.termuxagent.data.chat.ToolCallStatus
import com.termuxagent.data.chat.UiMessage
import com.termuxagent.data.chat.toWireFormat
import com.termuxagent.data.settings.AppSettings
import com.termuxagent.data.settings.SettingsStore
import com.termuxagent.data.workspace.WorkspaceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val isRunning: Boolean = false,
    val error: String? = null,
    val settings: AppSettings = AppSettings(),
    val needsConfig: Boolean = false
)

class ChatViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var runJob: Job? = null

    init {
        // Observe settings continuously so changes (API key, model, theme)
        // are reflected without restarting the screen.
        viewModelScope.launch {
            SettingsStore.flow(context).collect { s ->
                _state.update { it.copy(settings = s, needsConfig = !s.isConfigured) }
            }
        }
    }

    fun reloadSettings() {
        // No-op: settings are observed continuously. Kept for backward compat.
    }

    fun send(text: String) {
        if (text.isBlank() || _state.value.isRunning) return
        val current = _state.value
        if (!current.settings.isConfigured) {
            _state.update { it.copy(error = "Add your API key, base URL, and model in Settings first.") }
            return
        }
        // Append the user message immediately.
        val userMsg = UiMessage.User(text = text.trim())
        // Also append a placeholder assistant message we'll mutate as events arrive.
        val assistantId = java.util.UUID.randomUUID().toString()
        val assistantMsg = UiMessage.Assistant(
            id = assistantId,
            blocks = emptyList(),
            isStreaming = true
        )
        _state.update { it.copy(messages = it.messages + userMsg + assistantMsg, isRunning = true, error = null) }

        // Build the agent + run.
        val settings = current.settings
        val client = OpenAIClient(baseUrl = settings.baseUrl, apiKey = settings.apiKey)
        val registry = ToolRegistry(WorkspaceManager)
        val agent = Agent(settings = settings, registry = registry, client = client)
        val history = (_state.value.messages.dropLast(2)) // exclude the just-added user+assistant placeholders
            .toWireFormat()

        runJob = viewModelScope.launch {
            try {
                agent.run(history = history, userInput = text.trim()).collect { ev ->
                    handleAgentEvent(assistantId, ev)
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                finalizeAssistant(assistantId, cancelled = true)
                throw ce
            } catch (t: Throwable) {
                _state.update { it.copy(error = t.message ?: "Agent failed", isRunning = false) }
                finalizeAssistant(assistantId, error = t.message)
            } finally {
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    fun stop() {
        runJob?.cancel()
        runJob = null
        _state.update { it.copy(isRunning = false) }
    }

    fun clear() {
        if (_state.value.isRunning) stop()
        _state.update { it.copy(messages = emptyList(), error = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun handleAgentEvent(assistantId: String, ev: AgentEvent) {
        when (ev) {
            is AgentEvent.TextDelta -> {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id != assistantId || m !is UiMessage.Assistant) m
                        else {
                            // Find the last Text block (if streaming) and append, else create one.
                            val blocks = m.blocks.toMutableList()
                            val lastText = blocks.lastOrNull() as? AssistantBlock.Text
                            if (lastText != null && lastText.isStreaming) {
                                blocks[blocks.lastIndex] = lastText.copy(text = lastText.text + ev.text)
                            } else {
                                blocks.add(AssistantBlock.Text(text = ev.text, isStreaming = true))
                            }
                            m.copy(blocks = blocks)
                        }
                    })
                }
            }
            is AgentEvent.ToolCallStart -> {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id != assistantId || m !is UiMessage.Assistant) m
                        else {
                            // Finalize any streaming Text block.
                            val blocks = m.blocks.map { b ->
                                if (b is AssistantBlock.Text && b.isStreaming) b.copy(isStreaming = false) else b
                            }.toMutableList()
                            blocks.add(
                                AssistantBlock.ToolCall(
                                    toolCallId = ev.toolCallId,
                                    name = ev.name,
                                    argsRaw = "",
                                    status = ToolCallStatus.STREAMING
                                )
                            )
                            m.copy(blocks = blocks)
                        }
                    })
                }
            }
            is AgentEvent.ToolCallArgs -> {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id != assistantId || m !is UiMessage.Assistant) m
                        else {
                            val blocks = m.blocks.map { b ->
                                if (b is AssistantBlock.ToolCall && b.toolCallId == ev.toolCallId) {
                                    b.copy(argsRaw = ev.argsRaw)
                                } else b
                            }
                            m.copy(blocks = blocks)
                        }
                    })
                }
            }
            is AgentEvent.ToolCallRunning -> {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id != assistantId || m !is UiMessage.Assistant) m
                        else {
                            val blocks = m.blocks.map { b ->
                                if (b is AssistantBlock.ToolCall && b.toolCallId == ev.toolCallId) {
                                    b.copy(status = ToolCallStatus.RUNNING, argsRaw = ev.argsRaw)
                                } else b
                            }
                            m.copy(blocks = blocks)
                        }
                    })
                }
            }
            is AgentEvent.ToolResultEvent -> {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id != assistantId || m !is UiMessage.Assistant) m
                        else {
                            val blocks = m.blocks.map { b ->
                                if (b is AssistantBlock.ToolCall && b.toolCallId == ev.toolCallId) {
                                    b.copy(
                                        status = if (ev.ok) ToolCallStatus.DONE else ToolCallStatus.FAILED,
                                        result = ev.output,
                                        ok = ev.ok,
                                        meta = ev.meta
                                    )
                                } else b
                            }
                            m.copy(blocks = blocks)
                        }
                    })
                }
            }
            is AgentEvent.Iteration -> {
                // Could surface "thinking…" but we keep it implicit via tool cards.
            }
            is AgentEvent.Done -> {
                finalizeAssistant(assistantId)
            }
            is AgentEvent.Error -> {
                _state.update { it.copy(error = ev.message) }
                finalizeAssistant(assistantId, error = ev.message)
            }
        }
    }

    /** Mark the streaming assistant message as finalized. */
    private fun finalizeAssistant(assistantId: String, error: String? = null, cancelled: Boolean = false) {
        _state.update { st ->
            st.copy(
                isRunning = false,
                messages = st.messages.map { m ->
                    if (m.id != assistantId || m !is UiMessage.Assistant) m
                    else {
                        val blocks = m.blocks.map { b ->
                            when (b) {
                                is AssistantBlock.Text -> b.copy(isStreaming = false)
                                is AssistantBlock.ToolCall -> when (b.status) {
                                    ToolCallStatus.STREAMING, ToolCallStatus.RUNNING ->
                                        b.copy(status = ToolCallStatus.FAILED, result = b.result ?: "Interrupted")
                                    else -> b
                                }
                            }
                        }
                        // Drop trailing empty Text blocks (e.g. when only tool calls happened).
                        val cleaned = blocks.filterNot { it is AssistantBlock.Text && it.text.isBlank() }
                        m.copy(blocks = cleaned, isStreaming = false, error = error)
                    }
                }
            )
        }
    }
}

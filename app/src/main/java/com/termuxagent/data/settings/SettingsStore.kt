package com.termuxagent.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.termuxagent.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "termuxagent_settings")

/**
 * Per-tool-group toggle flags. Grouped (not per-tool) so the settings screen
 * stays uncrowded: casual users see 4 switches, not 16.
 *
 *  - toolsShellEnabled   : shell, list_interpreters
 *  - toolsFilesEnabled   : read_file, write_file, edit_file, append_file,
 *                          list_dir, tree, grep, mkdir, delete, file_info
 *  - toolsHttpEnabled    : http_fetch, download_url, web_read (note: web_read
 *                          pairs with web_search — if you turn web search on,
 *                          web_read stays available even if HTTP is off, so
 *                          the model can fetch search-result pages)
 *  - toolsShareEnabled   : share_file, open_url, copy_to_clipboard
 *
 * When a group is disabled, the agent never advertises those tools to the
 * model, so it cannot call them. Web search has its own master toggle
 * ([webSearchEnabled]) plus a provider pick.
 */
data class ToolToggles(
    val shell: Boolean = true,
    val files: Boolean = true,
    val http: Boolean = true,
    val share: Boolean = true
)

/**
 * Visual / behavioural options for the chat screen. Kept small on purpose —
 * the settings screen stays scannable.
 */
data class ChatUiOptions(
    val autoScroll: Boolean = true,
    val showTimestamps: Boolean = false,
    val messageTextSize: Int = 15   // sp, range 12..20
)

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxIterations: Int = 25,
    val temperature: Float = 0.3f,
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val webSearchEnabled: Boolean = true,
    val webSearchProvider: String = "duckduckgo",
    val exaApiKey: String = "",
    val firecrawlApiKey: String = "",
    val tavilyApiKey: String = "",
    val toolToggles: ToolToggles = ToolToggles(),
    val chatUi: ChatUiOptions = ChatUiOptions()
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    /**
     * Effective system prompt: if the user has cleared the field, fall back to
     * a minimal safe prompt. Many providers (OpenRouter, OpenAI) reject an
     * empty `system` message with HTTP 400/502, which surfaces in the UI as
     * a confusing "stream error 502". Falling back here fixes that at the
     * source.
     */
    val effectiveSystemPrompt: String
        get() = systemPrompt.trim().ifBlank { MINIMAL_SYSTEM_PROMPT }
}

const val DEFAULT_SYSTEM_PROMPT = """You are TermuXagent, an autonomous AI agent running on the user's Android phone. You have a real, persistent workspace — a folder you fully control. You can read and write files, run shell commands, search the workspace, fetch URLs, copy to clipboard, share files, and open URLs in the browser.

Behave like a senior engineer with full agency:
- Plan briefly before acting (1-3 short sentences), then call tools.
- Prefer the smallest tool call that makes progress. Don't try to do everything in one shot.
- After each tool result, decide: continue, verify, or summarize.
- When writing code, create the file with write_file, then run it with shell to verify it works. If it errors, read the error, fix, and retry — don't ask the user to do it.
- Keep file paths workspace-relative (e.g. "src/main.py"). The workspace root is the cwd for shell commands.
- Use list_interpreters to discover which runtimes (python3, node, ruby, etc.) are available before assuming one. If a language isn't installed, tell the user how to add it (e.g. via Termux) and proceed with a workaround (shell + toybox, or write code in a language that IS available).
- You have web_search and web_read tools. Use web_search to find current information on the internet, then web_read to fetch and understand a specific page. Always search before answering questions about recent events, APIs, or documentation you're unsure about.
- Be honest about limits. If something is impossible on a non-rooted Android device, say so and propose the closest alternative.
- When you're done, give the user a short summary of what you built, where the files are, and how to use them. Keep it tight.

Tone: direct, technical, friendly. No filler. Markdown is fine for the final summary."""

/** Used when the user clears the system prompt — minimal, neutral, never rejected. */
const val MINIMAL_SYSTEM_PROMPT = """You are a helpful, concise AI assistant running in an Android chat app. Answer the user's questions directly. Use Markdown for formatting when it improves readability. If you have tools available, use them only when they genuinely help answer the question."""

object SettingsStore {
    private val KEY_API_KEY = stringPreferencesKey("api_key")
    private val KEY_BASE_URL = stringPreferencesKey("base_url")
    private val KEY_MODEL = stringPreferencesKey("model")
    private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    private val KEY_MAX_ITER = intPreferencesKey("max_iterations")
    private val KEY_TEMP = stringPreferencesKey("temperature") // stored as string for fractional precision
    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
    private val KEY_WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
    private val KEY_WEB_SEARCH_PROVIDER = stringPreferencesKey("web_search_provider")
    private val KEY_EXA_API_KEY = stringPreferencesKey("exa_api_key")
    private val KEY_FIRECRAWL_API_KEY = stringPreferencesKey("firecrawl_api_key")
    private val KEY_TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
    // Tool group toggles
    private val KEY_TOOLS_SHELL = booleanPreferencesKey("tools_shell")
    private val KEY_TOOLS_FILES = booleanPreferencesKey("tools_files")
    private val KEY_TOOLS_HTTP = booleanPreferencesKey("tools_http")
    private val KEY_TOOLS_SHARE = booleanPreferencesKey("tools_share")
    // Chat UI options
    private val KEY_UI_AUTOSCROLL = booleanPreferencesKey("ui_autoscroll")
    private val KEY_UI_TIMESTAMPS = booleanPreferencesKey("ui_timestamps")
    private val KEY_UI_TEXT_SIZE = intPreferencesKey("ui_text_size")

    fun flow(context: Context): Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            apiKey = p[KEY_API_KEY] ?: "",
            baseUrl = p[KEY_BASE_URL]?.takeIf { it.isNotBlank() } ?: "https://api.openai.com/v1",
            model = p[KEY_MODEL]?.takeIf { it.isNotBlank() } ?: "gpt-4o-mini",
            systemPrompt = p[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT,
            maxIterations = p[KEY_MAX_ITER]?.takeIf { it in 1..100 } ?: 25,
            temperature = p[KEY_TEMP]?.toFloatOrNull()?.takeIf { it in 0f..2f } ?: 0.3f,
            themeMode = runCatching { ThemeMode.valueOf(p[KEY_THEME] ?: "System") }.getOrDefault(ThemeMode.System),
            dynamicColor = p[KEY_DYNAMIC] ?: false,
            webSearchEnabled = p[KEY_WEB_SEARCH_ENABLED] ?: true,
            webSearchProvider = p[KEY_WEB_SEARCH_PROVIDER] ?: "duckduckgo",
            exaApiKey = p[KEY_EXA_API_KEY] ?: "",
            firecrawlApiKey = p[KEY_FIRECRAWL_API_KEY] ?: "",
            tavilyApiKey = p[KEY_TAVILY_API_KEY] ?: "",
            toolToggles = ToolToggles(
                shell = p[KEY_TOOLS_SHELL] ?: true,
                files = p[KEY_TOOLS_FILES] ?: true,
                http = p[KEY_TOOLS_HTTP] ?: true,
                share = p[KEY_TOOLS_SHARE] ?: true
            ),
            chatUi = ChatUiOptions(
                autoScroll = p[KEY_UI_AUTOSCROLL] ?: true,
                showTimestamps = p[KEY_UI_TIMESTAMPS] ?: false,
                messageTextSize = p[KEY_UI_TEXT_SIZE]?.takeIf { it in 11..22 } ?: 15
            )
        )
    }

    suspend fun update(context: Context, transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val current = AppSettings(
                apiKey = p[KEY_API_KEY] ?: "",
                baseUrl = p[KEY_BASE_URL] ?: "https://api.openai.com/v1",
                model = p[KEY_MODEL] ?: "gpt-4o-mini",
                systemPrompt = p[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT,
                maxIterations = p[KEY_MAX_ITER] ?: 25,
                temperature = p[KEY_TEMP]?.toFloatOrNull() ?: 0.3f,
                themeMode = runCatching { ThemeMode.valueOf(p[KEY_THEME] ?: "System") }.getOrDefault(ThemeMode.System),
                dynamicColor = p[KEY_DYNAMIC] ?: false,
                webSearchEnabled = p[KEY_WEB_SEARCH_ENABLED] ?: true,
                webSearchProvider = p[KEY_WEB_SEARCH_PROVIDER] ?: "duckduckgo",
                exaApiKey = p[KEY_EXA_API_KEY] ?: "",
                firecrawlApiKey = p[KEY_FIRECRAWL_API_KEY] ?: "",
                tavilyApiKey = p[KEY_TAVILY_API_KEY] ?: "",
                toolToggles = ToolToggles(
                    shell = p[KEY_TOOLS_SHELL] ?: true,
                    files = p[KEY_TOOLS_FILES] ?: true,
                    http = p[KEY_TOOLS_HTTP] ?: true,
                    share = p[KEY_TOOLS_SHARE] ?: true
                ),
                chatUi = ChatUiOptions(
                    autoScroll = p[KEY_UI_AUTOSCROLL] ?: true,
                    showTimestamps = p[KEY_UI_TIMESTAMPS] ?: false,
                    messageTextSize = p[KEY_UI_TEXT_SIZE]?.takeIf { it in 11..22 } ?: 15
                )
            )
            val next = transform(current)
            p[KEY_API_KEY] = next.apiKey.trim()
            p[KEY_BASE_URL] = next.baseUrl.trim().trimEnd('/')
            p[KEY_MODEL] = next.model.trim()
            p[KEY_SYSTEM_PROMPT] = next.systemPrompt
            p[KEY_MAX_ITER] = next.maxIterations.coerceIn(1, 100)
            p[KEY_TEMP] = next.temperature.coerceIn(0f, 2f).toString()
            p[KEY_THEME] = next.themeMode.name
            p[KEY_DYNAMIC] = next.dynamicColor
            p[KEY_WEB_SEARCH_ENABLED] = next.webSearchEnabled
            p[KEY_WEB_SEARCH_PROVIDER] = next.webSearchProvider
            p[KEY_EXA_API_KEY] = next.exaApiKey.trim()
            p[KEY_FIRECRAWL_API_KEY] = next.firecrawlApiKey.trim()
            p[KEY_TAVILY_API_KEY] = next.tavilyApiKey.trim()
            p[KEY_TOOLS_SHELL] = next.toolToggles.shell
            p[KEY_TOOLS_FILES] = next.toolToggles.files
            p[KEY_TOOLS_HTTP] = next.toolToggles.http
            p[KEY_TOOLS_SHARE] = next.toolToggles.share
            p[KEY_UI_AUTOSCROLL] = next.chatUi.autoScroll
            p[KEY_UI_TIMESTAMPS] = next.chatUi.showTimestamps
            p[KEY_UI_TEXT_SIZE] = next.chatUi.messageTextSize.coerceIn(11, 22)
        }
    }
}

package com.termuxagent.data.agent

import com.termuxagent.data.agent.tools.AgentTool
import com.termuxagent.data.agent.tools.CopyClipboardTool
import com.termuxagent.data.agent.tools.DownloadUrlTool
import com.termuxagent.data.agent.tools.FileInfoTool
import com.termuxagent.data.agent.tools.OpenUrlTool
import com.termuxagent.data.agent.tools.ShareFileTool
import com.termuxagent.data.agent.tools.DeleteTool
import com.termuxagent.data.agent.tools.EditFileTool
import com.termuxagent.data.agent.tools.AppendFileTool
import com.termuxagent.data.agent.tools.GrepTool
import com.termuxagent.data.agent.tools.HttpFetchTool
import com.termuxagent.data.agent.tools.ListDirTool
import com.termuxagent.data.agent.tools.ListInterpretersTool
import com.termuxagent.data.agent.tools.MkdirTool
import com.termuxagent.data.agent.tools.ReadFileTool
import com.termuxagent.data.agent.tools.ShellTool
import com.termuxagent.data.agent.tools.TreeTool
import com.termuxagent.data.agent.tools.WebReadTool
import com.termuxagent.data.agent.tools.WebSearchTool
import com.termuxagent.data.agent.tools.WriteFileTool
import com.termuxagent.data.api.ToolDef
import com.termuxagent.data.api.ToolFunction
import com.termuxagent.data.settings.AppSettings
import com.termuxagent.data.workspace.WorkspaceManager
import kotlinx.serialization.json.JsonObject

/**
 * Holds the tool implementations and produces the OpenAI-style `tools` array
 * to send with chat requests. Respects the per-group toggles in
 * [AppSettings.toolToggles] — when a group is off, those tools are not
 * advertised to the model at all, so the model cannot call them.
 *
 * Web search has its own master toggle ([AppSettings.webSearchEnabled]) plus
 * a provider pick. `web_read` pairs with web search: it stays available
 * whenever web search is on, so the model can follow up on search results —
 * even if the HTTP-fetch group is toggled off.
 */
class ToolRegistry(ws: WorkspaceManager, settings: AppSettings) {
    private val tools: List<AgentTool> = buildList {
        // Shell + interpreters
        if (settings.toolToggles.shell) {
            add(ShellTool(ws))
            add(ListInterpretersTool())
        }
        // File operations
        if (settings.toolToggles.files) {
            add(ReadFileTool(ws))
            add(WriteFileTool(ws))
            add(EditFileTool(ws))
            add(AppendFileTool(ws))
            add(ListDirTool(ws))
            add(TreeTool(ws))
            add(GrepTool(ws))
            add(MkdirTool(ws))
            add(DeleteTool(ws))
            add(FileInfoTool(ws))
        }
        // HTTP fetch + download
        if (settings.toolToggles.http) {
            add(HttpFetchTool())
            add(DownloadUrlTool(ws))
        }
        // Share / open / clipboard
        if (settings.toolToggles.share) {
            add(CopyClipboardTool())
            add(ShareFileTool(ws))
            add(OpenUrlTool())
        }
        // Web search — its own master toggle, independent of the HTTP group
        if (settings.webSearchEnabled) {
            add(WebSearchTool(
                provider = settings.webSearchProvider,
                exaApiKey = settings.exaApiKey,
                firecrawlApiKey = settings.firecrawlApiKey,
                tavilyApiKey = settings.tavilyApiKey
            ))
            // web_read is the natural pair of web_search — keep it available
            // whenever web search is on, regardless of the HTTP toggle, so the
            // model can fetch the full content of any search-result URL.
            add(WebReadTool())
        }
    }

    private val byName: Map<String, AgentTool> = tools.associateBy { it.name }

    fun names(): List<String> = tools.map { it.name }

    fun has(name: String): Boolean = byName.containsKey(name)

    suspend fun invoke(name: String, args: JsonObject) =
        byName[name]?.invoke(args)
            ?: com.termuxagent.data.agent.tools.ToolResult(
                ok = false,
                output = "Unknown tool: $name. Available: ${names().joinToString(", ")}"
            )

    /** OpenAI-style tools array, serialised into the request body. */
    fun toolDefs(): List<ToolDef> = tools.map { t ->
        ToolDef(
            function = ToolFunction(
                name = t.name,
                description = t.description,
                parameters = t.parametersSchema
            )
        )
    }
}

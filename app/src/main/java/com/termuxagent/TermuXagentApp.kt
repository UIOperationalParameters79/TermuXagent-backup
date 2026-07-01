package com.termuxagent

import android.app.Application
import com.termuxagent.data.workspace.WorkspaceManager

class TermuXagentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise the singleton workspace root once, on app start.
        WorkspaceManager.init(this)
    }
}

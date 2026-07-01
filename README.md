# TermuXagent

> Your AI, your computer, in your pocket.

TermuXagent is a BYOK (Bring Your Own Key) AI agent for Android that gives any
OpenAI-compatible model a real, persistent on-device workspace. The agent can
read & write files, run shell commands, search the workspace, and fetch URLs —
iterating autonomously until your task is done, exactly like a desktop agent.

Built with Kotlin + Jetpack Compose + Material 3.

## Features

- **BYOK, OpenAI-compatible.** Point at any provider (OpenAI, OpenRouter,
  Together, Groq, local Llamafile, Ollama with the OpenAI shim, …) by setting
  Base URL + API key + model.
- **Streaming chat.** Server-sent events; the assistant's text and tool calls
  render live as they arrive.
- **Tool-calling agent loop.** The agent calls tools, sees results, and
  continues until it's done — up to a configurable max-iterations cap.
- **Real workspace.** A persistent folder the agent fully controls. Files
  survive app restarts and are browsable from the in-app Workspace tab.
- **Tools:** `shell`, `read_file`, `write_file`, `edit_file`, `append_file`,
  `list_dir`, `tree`, `grep`, `mkdir`, `delete`, `http_fetch`.
- **Clean, modern UI.** Material 3 with dynamic color (Android 12+), dark/light
  themes, smooth animations, expandable tool-call cards.

## Install

Grab the latest APK from the [Releases page](../../releases/latest), allow
"Install unknown apps" for your browser/files app on your phone, and tap to
install.

## First run

1. Open TermuXagent.
2. Tap ⚙️ → **Settings**.
3. Enter your **API Key**, **Base URL** (e.g. `https://api.openai.com/v1`),
   and **Model** (e.g. `gpt-4o-mini`).
4. Tap **Test connection** — you should see "OK — N models visible".
5. Go back to Chat and ask anything.

## Try these prompts

- *"Create a Python script that prints the first 20 Fibonacci numbers, then
  run it."* (Note: requires Python — see **Limits** below.)
- *"Write a Bash script that backs up every file in the workspace to a
  timestamped tar.gz, then run it."*
- *"Fetch https://api.github.com/repos/JetBrains/kotlin and write a 3-bullet
  summary of the repo to summary.md."*
- *"Set up a small static site: index.html, style.css, app.js. Make it a
  countdown timer to 2026-01-01."*

## Architecture

```
data/
  api/         OpenAI-compatible client + SSE parser + JSON models
  agent/       Agent loop + ToolRegistry
  agent/tools/ shell, file ops, grep, http_fetch
  chat/        UI message model + wire-format mapper
  settings/    DataStore-backed settings
  workspace/   Scoped filesystem manager
ui/
  theme/       Color, Type, Theme (Material 3, dynamic color)
  chat/        ChatScreen + ViewModel + message bubbles + tool cards
  workspace/   File browser + preview
  settings/    Settings form
  nav/         Single-activity NavHost
```

## Build from source

```bash
git clone https://github.com/UiOPERATIONALparameters/TermuXagent.git
cd TermuXagent
./gradlew assembleRelease
# APK at app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17 and Android SDK 34.

## Limits

- **Non-rooted Android.** Shell commands run via `/system/bin/sh` + Android's
  built-in `toybox`. That gives you `ls`, `cat`, `cp`, `mv`, `rm`, `mkdir`,
  `find`, `grep`, `sed`, `awk`, `gzip`, `tar`, `head`, `tail`, `sort`, `uniq`,
  `wc`, `bc`, `tr`, `cut`, `tee`, `xargs`, etc. — but **no `python`, `node`,
  `pip`, `apt`, or `curl`** unless you've separately installed them (e.g. via
  Termux:API, or by linking an interpreter into PATH).
- The agent is sandboxed to its app-private workspace (`Android/data/
  com.termuxagent/files/workspace`). It cannot access the rest of your phone's
  filesystem.
- Tool output is truncated (shell ~20KB, file reads ~256KB, http ~16KB) so a
  single tool call can't OOM the chat.

## License

MIT. See [LICENSE](LICENSE).

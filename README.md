# WIBES AI AUTOPILOT

> **THINK. COMMAND. AUTOMATE.**

**WIBES AI AUTOPILOT** is an autonomous personal AI agent application for Android powered by **Needle 3** by **Cactus Compute** as its primary reasoning and tool-calling engine.

Unlike chatbots or mock UI demonstrators, WIBES AI Autopilot executes multi-step Android tasks through natural-language commands, verifies real outcomes using system APIs, and maintains persistent personal memory.

---

## 1. Core Architecture

- **AI Brain**: **Needle 3** by Cactus Compute (`cactuscompute.com/needle#sandbox`)
  - Ultra-compact, on-device foundation model designed specifically for deterministic tool calling, structured JSON intent extraction, and multi-step plan decomposition.
  - Zero hallucination via byte-level grammar constraints.
  - Optional Cactus Compute sandbox / API endpoint integration.
- **Android Automation Layer**:
  - **Module A (App Control)**: Query installed apps, launch targets by package or name, deep-link to system settings.
  - **Module B (Screen Automation)**: `WibesAccessibilityService` for inspecting UI nodes, clicking elements, entering text, scrolling, and triggering global navigation gestures.
  - **Module C (File Management)**: Scoped storage search, storage audit, batch organizing by extension, and SHA-256 duplicate file hashing.
  - **Module D (Browser Automation)**: Intent-driven URL navigation, default search engine query execution.
  - **Module E (Notification Management)**: `WibesNotificationListenerService` capturing active notifications, categorizing by type, and providing on-device briefings.
  - **Module F (Communication)**: WhatsApp and Telegram workflow assistance, SMS drafting, and phone dialing.
  - **Module G (Termux Integration)**: Allowlisted, sandboxed command execution (`df -h`, `uptime`, `ping`).
- **Personal Memory**: SQLite/Room persistent knowledge base (`Preference`, `AppShortcut`, `CustomInstruction`, `Workflow`, `Fact`) with search, export, and import.
- **Task Scheduler**: Android `AlarmManager` exact RTC alarms with background receiver execution and status notifications.
- **Voice Assistant**: Integrated `SpeechRecognizer` for voice commands and `TextToSpeech` for auditory task outcome briefings.
- **Security**: Zero-Trust `ActionGuard` with real-time user authorization modals before any sensitive or destructive actions (file deletion, external communications, dialing, shell commands).

---

## 2. Supported Autonomous Scenarios

### Multi-Step File Organization & Storage Audit
> *"Find all APK files, organize them into a folder called Applications, and show me how much storage they occupy."*
1. **Needle 3 Decomposition**: Decomposes instruction into 4 ordered tool calls (`search_files`, `create_folder`, `organize_files_by_type`, `calculate_storage_usage`).
2. **Permission Check**: Validates file system access.
3. **Execution**: Dispatches each tool sequentially to `FileManager` and `FileOrganizer`.
4. **Verification**: Verifies directory creation, file count, and byte lengths.
5. **Report**: Delivers accurate storage metrics and file paths.

### Notification Briefing
> *"Summarize my active notifications and alerts"*
- Captures active notifications from `WibesNotificationListenerService`.
- Groups items into Communication, Alerts, Social, System, and Promotions.
- Reports briefing both visually and through voice synthesis.

### Duplicate File Cleanup
> *"Find duplicate files in Downloads using hash verification"*
- Calculates cryptographic SHA-256 hashes of files in Downloads.
- Reports duplicate sets and potential reclaimable disk space.

### Scheduled Background Workflows
> *"Every morning at 8:00 AM, show my important notifications"*
- Schedules recurring exact alarm via `AutomationScheduler`.
- Wakes up in background and executes the notification summary task automatically.

---

## 3. Security & Anti-Slop Implementation

- **No Pretended Outcomes**: Every action verifies its execution status. If an app fails to launch or a permission is absent, WIBES truthfully reports the error.
- **Zero Arbitrary Shell Execution**: Termux workflows are limited to a strictly enforced allowlist of safe commands.
- **Transparent Audit Trail**: Every tool invocation, parameter payload, confirmation decision, and verification result is logged to the local `audit_logs` Room table.

---

## 4. Build & Testing

- **Compile**: `gradle assembleDebug` or `compile_applet`
- **Unit & Robolectric Tests**: `gradle :app:testDebugUnitTest`
- **Target SDK**: Android 36 (Android 10+ compatible)
- **Architecture**: ARM64 optimized

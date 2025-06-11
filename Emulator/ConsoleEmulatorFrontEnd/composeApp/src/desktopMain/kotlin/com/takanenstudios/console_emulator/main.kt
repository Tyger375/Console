package com.takanenstudios.console_emulator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ConsoleEmulatorFrontEnd",
    ) {
        App()
    }
}
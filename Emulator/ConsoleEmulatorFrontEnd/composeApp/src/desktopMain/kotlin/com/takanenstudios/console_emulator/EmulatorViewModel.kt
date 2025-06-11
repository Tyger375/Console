package com.takanenstudios.console_emulator

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class EmulatorViewModel(
    private val repository: EmulatorRepository = EmulatorRepository()
) : ViewModel() {
    val dump = MutableStateFlow(ByteArray(0))

    fun getDump() {
        viewModelScope.launch {
            repository.getDump()
                ?.let {
                    dump.value = it
                }
        }
    }

    fun saveFile(text: String): File {
        val filesDir = File(System.getProperty("user.home"), ".console_emulator/files")
        if (!filesDir.exists())
            filesDir.mkdirs()

        val file = File(filesDir, "file.asm")
        if (!file.exists())
            file.createNewFile()

        file.writeText(text)

        return file
    }

    fun assembleAndLoad(
        text: String,
        onComplete: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = saveFile(text)
            repository.assembleAndLoad(res)
                ?.let(onFailure)
                ?: onComplete()
        }
    }
}
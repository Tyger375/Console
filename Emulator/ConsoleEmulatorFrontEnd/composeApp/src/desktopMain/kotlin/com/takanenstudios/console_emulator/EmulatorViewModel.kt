package com.takanenstudios.console_emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class EmulatorViewModel(
    private val repository: EmulatorRepository = EmulatorRepository()
) : ViewModel() {
    val registers = MutableStateFlow(Registers())
    val dump = MutableStateFlow(ByteArray(0))

    fun getDump() {
        viewModelScope.launch {
            repository.getDump()
                ?.let {
                    dump.value = it
                }
        }
    }

    fun saveParseResult(data: ParseResult) {
        val filesDir = File(System.getProperty("user.home"), ".console_emulator/files")
        if (!filesDir.exists())
            filesDir.mkdirs()

        val file = File(filesDir, "parse_result.json")
        if (!file.exists())
            file.createNewFile()

        file.writeText(Json.encodeToString(data))
    }

    fun loadParseResult(): ParseResult? = try {
        val filesDir = File(System.getProperty("user.home"), ".console_emulator/files")
        if (!filesDir.exists())
            filesDir.mkdirs()

        val file = File(filesDir, "parse_result.json")
        if (!file.exists())
            file.createNewFile()

        Json.decodeFromString(file.readText())
    } catch (_: Exception) {
        null
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

    fun loadFile(): String {
        val filesDir = File(System.getProperty("user.home"), ".console_emulator/files")
        if (!filesDir.exists())
            filesDir.mkdirs()

        val file = File(filesDir, "file.asm")
        if (!file.exists())
            file.createNewFile()

        return file.readText()
    }

    fun assembleAndLoad(
        text: String,
        onComplete: (ParseResult) -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            val res = saveFile(text)
            repository.assembleAndLoad(res)
                ?.let {
                    parseAssemblerOutput(it).let { pr ->
                        saveParseResult(pr)
                        onComplete(pr)
                    }
                }
                ?: onFailure()
        }
    }

    fun getRegisters(
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            repository.getRegisters()
                ?.let {
                    registers.value = it
                }
                ?: onFailure()
        }
    }

    fun restartEmulator(
        onComplete: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            if (repository.restartEmulator()) {
                registers.value = registers.value.copy(PC = 0)
                onComplete()
            }
            else onFailure()
        }
    }

    fun stepEmulator(
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (repository.stepEmulator()) {
                repository.getRegisters()
                    ?.let {
                        registers.value = it
                    }
                    ?: onFailure("Registers")
            }
            else onFailure("Stepping")
        }
    }
}
package com.takanenstudios.console_emulator

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Registers(
    val A: Int = 0,
    val B: Int = 0,
    val C: Int = 0,
    val D: Int = 0,
    val E: Int = 0,
    val F: Int = 0,
    val H: Int = 0,
    val L: Int = 0,
    val PC: Int = 0,
    val SP: Int = 0
)

class EmulatorRepository {
    suspend fun getDump(): ByteArray? = try {
        httpClient.get("$address/memory/dump")
            .let {
                if (it.status.isSuccess())
                    it.readRawBytes()
                else
                    null
            }
    } catch (e: Exception) {
        null
    }

    suspend fun assembleAndLoad(file: File): String? = try {
        httpClient.submitFormWithBinaryData(
            url =  "$address/emulator/assemble_and_load",
            formData = formData {
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"file.asm\"")
                })
            }
        ).body()
    } catch (e: Exception) {
        null
    }

    suspend fun restartEmulator(): Boolean = try {
        httpClient.get("$address/emulator/restart")
            .status.isSuccess()
    } catch (e: Exception) {
        false
    }

    suspend fun stepEmulator(): Boolean = try {
        httpClient.get("$address/emulator/step")
            .status.isSuccess()
    } catch (e: Exception) {
        false
    }

    suspend fun getRegisters(): Registers? = try {
        httpClient.get("$address/cpu/registers")
            .body()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
package com.takanenstudios.console_emulator

import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.File

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
        ).let {
            if (it.status.isSuccess())
                null
            else
                it.body()
        }
    } catch (e: Exception) {
        "INTERNAL"
    }
}
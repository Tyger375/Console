package com.takanenstudios.console_emulator

import kotlinx.serialization.Serializable

@Serializable
data class CodeLine(val address: Int, val bytes: String?, val instruction: String)
@Serializable
data class Error(val message: String, val address: Int)
@Serializable
data class Segment(val name: String, val address: Int, val size: Int)
@Serializable
data class Symbol(val name: String, val address: Int, val addressDec: Int, val file: String, val line: Int, val unused: Boolean)

@Serializable
data class ParseResult(
    val code: List<CodeLine>,
    val errors: List<Error>,
    val segments: List<Segment>,
    val symbols: List<Symbol>,
    val totalTime: Double?,
    val totalErrors: Int?
)

fun parseAssemblerOutput(input: String): ParseResult {
    val lines = input.lines()

    val codeLines = mutableListOf<CodeLine>()
    val errors = mutableListOf<Error>()
    val segments = mutableListOf<Segment>()
    val symbols = mutableListOf<Symbol>()

    var totalTime: Double? = null
    var totalErrors: Int? = null

    var section = "code"

    val codeRegex = Regex("""^([0-9A-F]{4}):\s*(?:([0-9A-F]+)\s+)?(.+)?$""")
    val segmentRegex = Regex("""#(\w+)\s+= \$([0-9A-F]+) =\s+(\d+),\s+size\s+= \$([0-9A-F]+) =\s+(\d+)""")
    val symbolRegex = Regex("""(\w+)\s+= \$([0-9A-F]+) =\s+(\d+)\s+(\S+):(\d+)(?:\s+\(unused\))?""")
    val timeRegex = Regex("""total time: ([0-9.]+) sec\.""")
    val errorCountRegex = Regex("""(\d+) error""")

    var codeFound = false
    var codeIndex = -1

    for (line in lines) {
        when {
            line.startsWith("; +++ segments") -> section = "segments"
            line.startsWith("; +++ global symbols") -> section = "symbols"
            line.startsWith("total time:") -> {
                val match = timeRegex.find(line)
                if (match != null) totalTime = match.groupValues[1].toDouble()
            }
            errorCountRegex.matches(line) -> {
                val match = errorCountRegex.find(line)
                if (match != null) totalErrors = match.groupValues[1].toInt()
            }
            section == "code" -> {
                val match = codeRegex.find(line)
                if (match != null) {
                    codeFound = true
                    val addr = match.groupValues[1].toInt(16)
                    val bytes = match.groupValues[2].ifBlank { null }
                    val instr = match.groupValues[3].trim()
                    codeLines.add(CodeLine(addr, bytes, instr))
                }
                else if (line.startsWith("***ERROR***")) {
                    errors.add(Error(line, codeIndex))
                    continue
                }

                if (codeFound)
                    codeIndex++
            }
            section == "segments" -> {
                val match = segmentRegex.find(line)
                if (match != null) {
                    segments.add(
                        Segment(
                            name = match.groupValues[1],
                            address = match.groupValues[2].toInt(16),
                            size = match.groupValues[4].toInt(16)
                        )
                    )
                }
            }
            section == "symbols" -> {
                val match = symbolRegex.find(line)
                if (match != null) {
                    symbols.add(
                        Symbol(
                            name = match.groupValues[1],
                            address = match.groupValues[2].toInt(16),
                            addressDec = match.groupValues[3].toInt(),
                            file = match.groupValues[4],
                            line = match.groupValues[5].toInt(),
                            unused = line.contains("(unused)")
                        )
                    )
                }
            }
        }
    }

    return ParseResult(
        code = codeLines,
        errors = errors,
        segments = segments,
        symbols = symbols,
        totalTime = totalTime,
        totalErrors = totalErrors
    )
}
package com.takanenstudios.console_emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.full.memberProperties

const val bytesPerRow = 16
val offsetCellWidth = 50.dp
val cellWidth = 35.dp
val gridPadding = 4.dp
val rowHeight = 30.dp

@Composable
fun HexEditor(
    modifier: Modifier = Modifier,
    dump: ByteArray,
    onReload: () -> Unit = {}
) {
    if (dump.size != 0x10000) return

    val bytesPerPage = 0x1000

    var currentPage by remember { mutableStateOf(0) }
    val start = currentPage * bytesPerPage
    val pageData = remember(dump, currentPage) { dump.sliceArray(start until (start + bytesPerPage)) }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentPage > 0)
                        currentPage--
                },
                enabled = currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            Text(
                "Page ${currentPage + 1}/16 (0x%04X - 0x%04X)".format(start, start + bytesPerPage - 1),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = {
                    if (currentPage < 15)
                        currentPage++
                },
                enabled = currentPage < 15
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
            Button(onClick = onReload) { Text("Reload") }
        }

        HexGrid(dump = pageData, offset = start)
    }
}

@Composable
fun HexGrid(
    dump: ByteArray,
    offset: Int
) {
    val rowCount = dump.size / bytesPerRow
    val density = LocalDensity.current

    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .width(700.dp)
            .heightIn(max = 400.dp)
            .border(1.dp, Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offsetPos ->
                        val index = pointerToByteIndex(offsetPos, rowCount, density)
                        if (index != null) {
                            selectionStart = index
                            selectionEnd = index
                        }
                    },
                    onDrag = { change, _ ->
                        val index = pointerToByteIndex(change.position, rowCount, density)
                        if (index != null) {
                            selectionEnd = index
                        }
                    }
                )
            }
            .padding(8.dp)
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rowCount) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gridPadding),
                    modifier = Modifier.height(rowHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%04X".format(offset + row * bytesPerRow),
                        modifier = Modifier.width(offsetCellWidth),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    for (col in 0 until bytesPerRow) {
                        val byteIndex = row * bytesPerRow + col
                        val selected = selectionStart != null && selectionEnd != null &&
                                byteIndex in (min(selectionStart!!, selectionEnd!!) .. max(selectionStart!!, selectionEnd!!))

                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(rowHeight)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    MaterialTheme.shapes.extraSmall
                                )
                                .clickable {
                                    if (selectionStart == byteIndex && selectionEnd == byteIndex) {
                                        selectionStart = null
                                        selectionEnd = null
                                    } else {
                                        selectionStart = byteIndex
                                        selectionEnd = byteIndex
                                    }
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (byteIndex < dump.size) "%02X".format(dump[byteIndex]) else "",
                                fontFamily = FontFamily.Monospace,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utility to map pointer offset to byte index.
 */
fun pointerToByteIndex(position: Offset, rowCount: Int, density: Density): Int? = with(density) {
    val rowY = (rowHeight.toPx() + gridPadding.toPx())
    val colX = (cellWidth.toPx() + gridPadding.toPx())

    val row = floor(position.y / rowY).toInt()
    val col = floor((position.x - offsetCellWidth.toPx() - gridPadding.toPx()) / colX).toInt()

    if (row < 0 || row >= rowCount || col < 0 || col >= bytesPerRow) return null

    return row * bytesPerRow + col
}

val TAB_SIZE = 4

fun insertTab(oldText: String, selection: TextRange): Pair<String, TextRange> {
    val tabSpaces = " ".repeat(TAB_SIZE)
    val newText = oldText.replaceRange(selection.start, selection.end, tabSpaces)
    val newSelection = TextRange(selection.start + TAB_SIZE)
    return Pair(newText, newSelection)
}

data class CodeData(
    val startAddress: Int,
    val codeSize: Int
)

@Composable
fun AsmIde(
    modifier: Modifier = Modifier,
    viewModel: EmulatorViewModel
) {
    var oldText by remember { mutableStateOf("") }

    val registers by viewModel.registers.collectAsState()
    var textState by remember(oldText) { mutableStateOf(TextFieldValue(oldText)) }

    val instructions = remember { mutableStateListOf<CodeLine>() }
    var codeData by remember { mutableStateOf<CodeData?>(null) }
    val errorLines = remember { mutableStateListOf<Int>()}

    val handleParseResult: (ParseResult) -> Unit = {
        println(it)
        instructions.clear()
        instructions.addAll(it.code)
        it.segments.singleOrNull { s -> s.name == "CODE" }
            ?.let { s ->
                codeData = CodeData(
                    startAddress = s.address,
                    codeSize = s.size
                )
            }
        errorLines.clear()
        errorLines.addAll(it.errors.map { e -> e.address })
    }

    LaunchedEffect(Unit) {
        oldText = viewModel.loadFile()
        viewModel.loadParseResult()?.let(handleParseResult)
    }

    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.assembleAndLoad(
                        textState.text,
                        onComplete = handleParseResult,
                        onFailure = {
                            println("ERROR")
                        }
                    )
                }
            ) {
                Text("Load")
            }

            if (errorLines.isEmpty())
                Text("OK")

            Row {
                Button(
                    onClick = {
                        viewModel.restartEmulator(
                            onComplete = {

                            },
                            onFailure = {

                            }
                        )
                    }
                ) {
                    Text("Restart")
                }

                Button(
                    onClick = {
                        viewModel.stepEmulator(
                            onFailure = {
                                println("Failed on $it")
                            }
                        )
                    }
                ) {
                    Text("Step")
                }
            }
        }
        AsmEditor(
            textState,
            onValueChange = {
                textState = it
            },
            instructions = instructions,
            errorLines = errorLines,
            currentLine  = registers.PC
        )
    }
}

enum class TokenType {
    LABEL, OPCODE, DIRECTIVE, COMMENT, NUMBER, ERROR, TEXT
}

data class Token(val text: String, val type: TokenType)

fun tokenizeAsmLine(line: String): List<Token> {
    val tokens = mutableListOf<Token>()

    val trimmed = line.trimStart()

    if (trimmed.startsWith(";")) {
        tokens.add(Token(trimmed, TokenType.COMMENT))
        return tokens
    }

    // Simple label detection
    val labelRegex = Regex("""^(\w+):""")
    val labelMatch = labelRegex.find(trimmed)
    if (labelMatch != null) {
        tokens.add(Token(labelMatch.groupValues[1] + ":", TokenType.LABEL))
        val rest = trimmed.substring(labelMatch.range.last + 1).trimStart()
        tokens.addAll(tokenizeAsmLine(rest))
        return tokens
    }

    // Split remaining by spaces
    val parts = trimmed.split(Regex("\\s+"))
    for (part in parts) {
        when {
            part.startsWith(".") -> tokens.add(Token(part, TokenType.DIRECTIVE))
            part.matches(Regex("[A-Za-z]{2,}")) -> tokens.add(Token(part, TokenType.OPCODE))
            part.matches(Regex("0x[0-9A-Fa-f]+|\\d+")) -> tokens.add(Token(part, TokenType.NUMBER))
            else -> tokens.add(Token(part, TokenType.TEXT))
        }
    }

    return tokens
}

@Composable
fun AsmEditor(
    textState: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    instructions: List<CodeLine>,
    errorLines: List<Int>,
    currentLine: Int
) {
    Row(
        modifier = Modifier
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val annotated = buildAnnotatedString {
            val lines = textState.text.lines()

            for ((index, _) in lines.withIndex()) {
                withStyle(
                    style = SpanStyle(
                        color = Color.Gray
                    )
                ) {
                    append("$index")
                }

                append("  ")

                val instruction = instructions.getOrNull(index)
                if (instruction != null && instruction.bytes != null && instruction.address == currentLine) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("•")
                    }
                } else {
                    append(" ")
                }

                append("\n")
            }
        }

        Text(
            annotated,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Box(
            modifier = Modifier
                .width(500.dp)
        ) {
            // Draw highlighted text behind the text field
            SyntaxHighlightedAsmText(
                text = textState.text,
                errorLines = errorLines
            )

            // Transparent text field over it
            BasicTextField(
                value = textState,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    color = Color.Transparent, // Make actual input text transparent
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .padding(top = 1.dp)
                    .fillMaxSize()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown) {
                            val (newText, newSelection) = insertTab(textState.text, textState.selection)
                            onValueChange(textState.copy(text = newText, selection = newSelection))
                            true
                        } else false
                    },
            )
        }
    }
}

@Composable
fun SyntaxHighlightedAsmText(
    text: String,
    errorLines: List<Int>
) {
    val annotated = buildAnnotatedString {
        val lines = text.lines()

        for ((index, line) in lines.withIndex()) {
            val leadingSpacesCount = line.takeWhile { it == ' ' }.count()
            val leadingSpaces = "\u00A0".repeat(leadingSpacesCount)
            val trimmedLine = line.drop(leadingSpacesCount)

            if (errorLines.contains(index)) {
                // Highlight whole line with background, preserving indentation
                withStyle(SpanStyle(background = Color(0xFFFFCCCC))) {
                    append(leadingSpaces)
                    append(trimmedLine)
                }
            } else {
                // Highlight tokens normally, but preserve indentation as non-breaking spaces
                append(leadingSpaces)
                val tokens = tokenizeAsmLine(line)
                for (token in tokens) {
                    withStyle(when (token.type) {
                        TokenType.LABEL -> SpanStyle(color = Color(0xFF007ACC))
                        TokenType.OPCODE -> SpanStyle(color = Color(0xFFDD4A68))
                        TokenType.DIRECTIVE -> SpanStyle(color = Color(0xFF795E26))
                        TokenType.NUMBER -> SpanStyle(color = Color(0xFF098658))
                        TokenType.COMMENT -> SpanStyle(color = Color(0xFF6A9955))
                        else -> SpanStyle(color = Color.Black)
                    }) {
                        append(token.text)
                    }
                    append(" ") // append the space AFTER the token to match spacing in original
                }
            }
            append("\n")
        }
    }

    Text(
        annotated,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun RegistersTable(
    modifier: Modifier = Modifier,
    viewModel: EmulatorViewModel
) {
    val registers by viewModel.registers.collectAsState()
    LazyColumn(
        modifier = modifier
    ) {
        items(
            registers::class
                .memberProperties
                .map { it.name to it.getter.call(registers) as Int }
        ) {
            Row(
                modifier = Modifier
                        .width(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(it.first)
                Text(
                    "0x%02X".format(it.second),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
@Preview
fun App() {
    val viewModel = viewModel {
        EmulatorViewModel()
    }

    var refreshDump by remember { mutableStateOf(false) }

    LaunchedEffect(refreshDump) {
        viewModel.getDump()
    }

    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AsmIde(
                modifier = Modifier
                    .weight(1.5f),
                viewModel
            )

            val dump by viewModel.dump.collectAsState()
            HexEditor(
                modifier = Modifier
                    .weight(2f),
                dump,
                onReload = {
                    refreshDump = !refreshDump
                }
            )

            RegistersTable(
                modifier = Modifier
                    .weight(1f),
                viewModel
            )
        }
    }
}
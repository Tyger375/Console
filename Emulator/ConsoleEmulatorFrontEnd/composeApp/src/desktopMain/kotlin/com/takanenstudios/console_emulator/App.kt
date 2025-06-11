package com.takanenstudios.console_emulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

val offsetCellWidth = 50.dp
val cellWidth = 35.dp
val gridPadding = 4.dp

const val bytesPerRow = 16

@Composable
fun DumpTable(
    dump: ByteArray,
    onReloadDump: () -> Unit
) {
    if (dump.size != 0x10000) return

    val bytesPerRow = 16
    val bytesPerPage = 0x1000

    var currentPage by remember { mutableStateOf(0) }
    val start = currentPage * bytesPerPage
    val pageData = remember(dump) { dump.sliceArray(start until (start + bytesPerPage)) }

    Column {
        // Page Controls
        Row {
            Row(
                modifier = Modifier
                    .width(offsetCellWidth + cellWidth * bytesPerRow + gridPadding * (bytesPerRow - 1))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Text("Previous")
                }

                Text(
                    text = "Page ${currentPage + 1}/16 (0x%04X - 0x%04X)".format(start, start + bytesPerPage - 1),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Button(
                    onClick = { if (currentPage < 15) currentPage++ },
                    enabled = currentPage < 15
                ) {
                    Text("Next")
                }
            }

            Button(
                onClick = onReloadDump
            ) {
                Text("Reload")
            }
        }

        DumpGrid(dump = pageData, offset = start)
    }
}

@Composable
fun DumpGrid(dump: ByteArray, offset: Int = 0) {
    val rowCount = dump.size / bytesPerRow

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(rowCount) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(gridPadding)
            ) {
                Text(
                    text = "%04X".format(offset + row * bytesPerRow),
                    modifier = Modifier.width(offsetCellWidth),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                for (col in 0 until bytesPerRow) {
                    val byteIndex = row * bytesPerRow + col
                    Text(
                        text = if (byteIndex < dump.size) "%02X".format(dump[byteIndex]) else "",
                        modifier = Modifier.width(cellWidth),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

val TAB_SIZE = 4

fun insertTab(oldText: String, selection: TextRange): Pair<String, TextRange> {
    val tabSpaces = " ".repeat(TAB_SIZE)
    val newText = oldText.replaceRange(selection.start, selection.end, tabSpaces)
    val newSelection = TextRange(selection.start + TAB_SIZE)
    return Pair(newText, newSelection)
}

@Composable
fun AsmIde(
    viewModel: EmulatorViewModel
) {
    var textState by remember { mutableStateOf(TextFieldValue()) }


    Column {
        Button(
            onClick = {
                viewModel.assembleAndLoad(
                    textState.text,
                    onComplete = {

                    },
                    onFailure = {
                        println(it)
                    }
                )
            }
        ) {
            Text("Load")
        }
        AsmEditor(
            textState,
            onValueChange = {
                textState = it
            }
        )
    }
}

@Composable
fun AsmEditor(
    textState: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .width(500.dp)
            .padding(16.dp)
    ) {
        // Line numbers
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .width(50.dp)
        ) {
            val lines = textState.text.lines().count()
            for (i in 1..lines) {
                Row(
                    modifier = Modifier
                        .height(20.dp)
                ) {
                    Text(
                        text = i.toString(),
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        lineHeight = 20.sp,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Code editor
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 2.dp)
        ) {
            BasicTextField(
                value = textState,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color.Black,
                    lineHeight = 20.sp // slightly lower than 25.sp, experiment here
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown) {
                            val (newText, newSelection) = insertTab(textState.text, textState.selection)
                            onValueChange(textState.copy(text = newText, selection = newSelection))
                            true
                        } else false
                    }
            )
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
        Row {
            AsmIde(viewModel)

            val dump by viewModel.dump.collectAsState()
            DumpTable(
                dump,
                onReloadDump = {
                    refreshDump = !refreshDump
                }
            )
        }
    }
}
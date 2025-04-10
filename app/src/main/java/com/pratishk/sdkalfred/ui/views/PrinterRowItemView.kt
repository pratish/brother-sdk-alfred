package com.pratishk.sdkalfred.ui.views

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.pratishk.sdkalfred.ui.ConnectionStatus
import com.pratishk.sdkalfred.ui.PrintJobStatus
import com.pratishk.sdkalfred.ui.PrinterModel

@Composable
fun PrinterRowItemView(printer: PrinterModel) {
    val connectionStatus = remember { printer.connectionStatus }
    val TAG = "PrinterRowItemView"
    Row(
        modifier = Modifier.padding(16.dp)
    ) {
        Column {
            Text(printer.channel.channelInfo.toString())
            Text(
                fontWeight = FontWeight.Thin,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                text = "Connection Status: ${connectionStatus.value}"
            )
        }
        Spacer(modifier = Modifier.weight(1.0f))
        if (printer.printJobStatus.value == PrintJobStatus.Empty || printer.printJobStatus.value is PrintJobStatus.Completed) {
            when (connectionStatus.value) {
                ConnectionStatus.Discovered,
                ConnectionStatus.FailedToConnect -> {
                    Button(
                        onClick = {printer.connect()}
                    ) {
                        Text("Connect")
                    }
                }
                ConnectionStatus.Connecting -> {
                    PrinterModelProgressIndicator()
                }
                ConnectionStatus.Connected -> {
                    Button(
                        onClick = { printer.printPDF() }
                    ) {
                        Text("Print PDF")
                    }
                }
            }
        } else {
            PrinterModelProgressIndicator()
        }
    }
}

@Composable
fun PrinterModelProgressIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(25.dp)
    )
}
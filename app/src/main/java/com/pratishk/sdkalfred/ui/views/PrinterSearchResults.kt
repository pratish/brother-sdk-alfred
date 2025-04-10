package com.pratishk.sdkalfred.ui.views

import android.util.Printer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brother.sdk.lmprinter.Channel
import com.pratishk.sdkalfred.ui.PrinterModel

@Composable
fun PrinterSearchResults(
    channels: List<PrinterModel>
) {
    Column {
        if (channels.isEmpty()) {
            Text("No Printer Found!")
        }
        channels.forEach {
            PrinterRowItemView(it)
        }
    }
}
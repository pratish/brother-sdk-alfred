package com.pratishk.sdkalfred

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brother.sdk.lmprinter.BLESearchOption
import com.brother.sdk.lmprinter.PrinterDriver
import com.brother.sdk.lmprinter.PrinterSearcher
import com.pratishk.sdkalfred.ui.AndroidPDFFilePicker
import com.pratishk.sdkalfred.ui.theme.SDKAlfredTheme
import com.pratishk.sdkalfred.ui.views.PrinterSearchResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = CoroutineScope(Dispatchers.IO)

        val picker = AndroidPDFFilePicker { callback ->
            registerForActivityResult(ActivityResultContracts.OpenDocument(), callback)
        }
        setContent {
            SDKAlfredTheme {
                enableEdgeToEdge()
                val printerList =
                    remember { mutableStateListOf<com.pratishk.sdkalfred.ui.PrinterModel>() }
                val option = BLESearchOption(5.0)
                PrinterSearcher.startBLESearch(applicationContext, option) { channel ->
                    if (printerList.firstOrNull { it.channel.channelInfo == channel.channelInfo } == null) {
                        printerList.add(
                            com.pratishk.sdkalfred.ui.PrinterModel(
                                channel = channel,
                                scope = scope,
                                context = applicationContext,
                                filePicker = picker
                            )
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("SFPD - BLE Test")
                            }
                        )
                    }
                ) { innerPadding ->
                    BluetoothPermissionScreen()
                    Column(
                        modifier = Modifier.padding(innerPadding).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PrinterSearchResults(
                            printerList
                        )
                    }
                }
            }
        }
    }
}

//               printerDriver?.sendRawData(hexToByteArray("1B6961001B401B694C011B28430200C7031B24CB001B28560200CB001B6B0B1B58006400417420796F757220736964650C"))
package com.pratishk.sdkalfred.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.print.PrintJob
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.brother.sdk.lmprinter.Channel
import com.brother.sdk.lmprinter.Error
import com.brother.sdk.lmprinter.OpenChannelError
import com.brother.sdk.lmprinter.PrintError
import com.brother.sdk.lmprinter.PrinterDriver
import com.brother.sdk.lmprinter.PrinterDriverGenerator
import com.brother.sdk.lmprinter.PrinterModel
import com.brother.sdk.lmprinter.setting.CustomPaperSize
import com.brother.sdk.lmprinter.setting.PJPaperSize
import com.brother.sdk.lmprinter.setting.PJPaperSize.PaperSize
import com.brother.sdk.lmprinter.setting.PJPrintSettings
import com.brother.sdk.lmprinter.setting.PrintSettings
import com.brother.sdk.lmprinter.setting.RJPrintSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.jvm.Throws

interface PDFFilePicker {
    fun pickPDF(onPDFPicked: (Uri) -> Unit)
}

class AndroidPDFFilePicker(
    registerLauncher: (ActivityResultCallback<Uri?>) -> ActivityResultLauncher<Array<String>>
) : PDFFilePicker {

    private var onPDFPickedCallback: ((Uri) -> Unit)? = null
    private val launcher = registerLauncher { uri -> handleResult(uri) }

    override fun pickPDF(onPDFPicked: (Uri) -> Unit) {
        onPDFPickedCallback = onPDFPicked
        launcher.launch(arrayOf("application/pdf"))
    }

    private fun handleResult(uri: Uri?) {
        uri?.let { onPDFPickedCallback?.invoke(it) }
        onPDFPickedCallback = null
    }
}


class PrinterModel(
    val channel: Channel,
    private val scope: CoroutineScope,
    val filePicker: AndroidPDFFilePicker,
    val context: Context,
    private val onStateChanged: ((ConnectionStatus) -> Unit)? = null
) {
    private val TAG = "PrinterModel_${channel.channelInfo}"
    private val printSettingsProvider: PrintSettingsProvider = DefaultPrintSettingsBuilder()
    var connectionStatus: MutableState<ConnectionStatus> = mutableStateOf(ConnectionStatus.Discovered)
        private set

    var printJobStatus: MutableState<PrintJobStatus> = mutableStateOf(PrintJobStatus.Empty)
        private set

    private var job: Job? = null

    private var driver: PrinterDriver? = null
        private set

    fun connect() {
        Log.d(TAG, "connect: Trying to connect")
        connectionStatus.value = ConnectionStatus.Connecting
        scope.launch {
            val result = PrinterDriverGenerator.openChannel(channel)
            driver = when (result.error.code) {
                OpenChannelError.ErrorCode.NoError -> {
                    Log.d(TAG, "connect: Connected with NoError")
                    connectionStatus.value = ConnectionStatus.Connected
                    result.driver
                }
                else -> {
                    connectionStatus.value = ConnectionStatus.FailedToConnect
                    Log.d(TAG, "connect: Connection Failed")
                    null
                }
            }
        }
    }

    @Throws(PrinterModelError::class)
    fun printPDF() {
        if (isBusy()) throw PrinterModelError.AlreadyPrinting
        if (ConnectionStatus.Connected != connectionStatus.value) throw PrinterModelError.NotConnected
        val driver = driver ?: throw PrinterModelError.NotConnected

        filePicker.pickPDF { uri ->
            val file = File(context.externalCacheDir, "temp.pdf")
            val filePath = file.absolutePath

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@pickPDF
            with(inputStream) {
                FileOutputStream(file).use {
                    this.copyTo(it)
                }
            }
            Log.d(TAG, "printPDF: print file saved in app space. Starting Print Job")
            job = scope.launch(Dispatchers.IO) {
                try {
                    val printSettings = printSettingsProvider.getDefaultPrintSettings(PrinterModel.PJ_883, context.filesDir.path)
                    val result = driver.printPDF(filePath, printSettings)
                    if (result.code == PrintError.ErrorCode.NoError) {
                        Log.d(TAG, "printPDF: Print Successful")
                    } else {
                        Log.d(TAG, "printPDF: Print Failed with error ${result.allLogs}")
                    }
                    driver.closeChannel()
                } catch (e: Exception) {
                    println(e)
                    Log.d(TAG, "printPDF: Exception occurred. $e")
                } finally {
                    printJobStatus.value = PrintJobStatus.Completed(job)
                    Log.d(TAG, "printPDF: Completed print job. Closing all channels")
                    inputStream.close()
                    job = null
                    file.delete()
                }
            }
            job?.let {
                printJobStatus.value = PrintJobStatus.PrintStarted(it)
            }
        }
    }

    private fun isBusy(): Boolean {
        return !(printJobStatus.value == PrintJobStatus.Empty || printJobStatus.value is PrintJobStatus.Completed)
    }
}

enum class ConnectionStatus {
    Connecting,
    Connected,
    FailedToConnect,
    Discovered
}

sealed class PrinterModelError(msg: String) : Throwable(msg) {
    data object NotConnected : PrinterModelError("Printer not connected")
    data object AlreadyPrinting: PrinterModelError("Printer is already processing a print job")
}

sealed class PrintJobStatus {
    data object Empty: PrintJobStatus()
    class Queued(job: Job?): PrintJobStatus()
    class PrintStarted(job: Job?): PrintJobStatus()
    class Completed(job: Job?): PrintJobStatus()
}

interface PrintSettingsProvider {
    fun getDefaultPrintSettings(printerModel: PrinterModel, workPathDirectory: String): PrintSettings?
}

class DefaultPrintSettingsBuilder: PrintSettingsProvider {
    override fun getDefaultPrintSettings(printerModel: PrinterModel, workPathDirectory: String): PrintSettings? {
        Log.d("PrintSettingsBuilder", "getDefaultPrintSettings: model: $printerModel")
        return when (printerModel) {
            PrinterModel.PJ_883 -> PJPrintSettings(PrinterModel.PJ_883).apply {
                paperSize = PJPaperSize.newPaperSize(PaperSize.Letter)
                workPath = workPathDirectory
            }
            PrinterModel.RJ_4250WB -> RJPrintSettings(PrinterModel.RJ_4250WB).apply {
                workPath = workPathDirectory
                density = RJPrintSettings.Density.StrongLevel5
                customPaperSize = CustomPaperSize.newRollPaperSize(4.0f, CustomPaperSize.Margins(0f,0f, 0f, 0f), CustomPaperSize.Unit.Inch, 15)
            }
            else -> return null
        }
    }
}
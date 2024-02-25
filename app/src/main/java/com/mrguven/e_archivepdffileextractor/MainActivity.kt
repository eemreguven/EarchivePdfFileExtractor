package com.mrguven.e_archivepdffileextractor

import AppFileUtils
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let { AppFileUtils.getPathFromUri(this, it)?.let { it1 -> convertFile(it1) } }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.openFileButton).setOnClickListener {
            openFilePicker()
        }
    }


    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        pickFileLauncher.launch(intent)
    }

    private fun convertFile(filePath: String) {
        val file = File(filePath)
        if (file.exists() && file.name.endsWith(".zip.json")) {
            val newFile = File(file.parentFile, ZIP_FILE_NAME)

            if (file.renameTo(newFile)) {
                showToast(R.string.toast_file_converted, file.name, ZIP_FILE_NAME)
                performConvertionOperations(newFile)
            } else {
                showToast(R.string.toast_conversion_failed, file.name)
            }
        } else {
            showToast(R.string.toast_invalid_file_selected)
        }
    }

    private fun performConvertionOperations(file: File) {
        val htmlContent = extractHtmlFileContentFromZip(file)
        htmlContent?.let { convertHtmlToPdf(it) }
    }

    private fun extractHtmlFileContentFromZip(zipFile: File): String? {
        val buffer = ByteArray(1024)
        val zipInputStream = ZipInputStream(FileInputStream(zipFile))
        var htmlContent: String? = null

        try {
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".html")) {
                    val stringBuilder = StringBuilder()
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        stringBuilder.append(String(buffer, 0, len, Charsets.UTF_8))
                    }
                    htmlContent = stringBuilder.toString()
                    zipInputStream.closeEntry()
                    showToast(R.string.toast_html_content_extracted)
                    break // Assuming there's only one HTML file, exit loop after finding it
                }
                entry = zipInputStream.nextEntry
            }
        } finally {
            zipInputStream.close()
            if (htmlContent == null) {
                showToast(R.string.toast_html_file_not_found)
            }
        }
        return htmlContent
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun convertHtmlToPdf(htmlContent: String) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true // Enable JavaScript if needed
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        printWebView(webView)
    }

    private fun printWebView(webView: WebView) {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(PDF_FILE_NAME)
        val jobName = getString(R.string.app_name) + " Print"
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.JIS_EXEC) // Set paper size here
            .build()
        printManager.print(jobName, printAdapter, printAttributes)
    }

    private fun showToast(messageResId: Int, vararg formatArgs: Any) {
        val message = getString(messageResId, *formatArgs)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ZIP_FILE_NAME = "fatura.zip"
        const val PDF_FILE_NAME = "Fatura"
    }
}
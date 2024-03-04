package com.mrguven.e_archivepdffileextractor

import android.annotation.SuppressLint
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mrguven.e_archivepdffileextractor.databinding.ActivityConvertBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ConvertActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConvertBinding

    private lateinit var filePath: String
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConvertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePath = intent.getStringExtra(MainActivity.FILE_PATH_KEY) ?: ""

        lifecycleScope.launch {
            delay(1000)
            binding.progressBar.visibility = View.GONE
            performConversionOperations(filePath)
        }
    }

    private fun performConversionOperations(filePath: String?) {
        selectedFile = renameFileAsZip(filePath)
        selectedFile?.let { file ->
            val htmlContent = extractHtmlFileContentFromZip(file)
            htmlContent?.let { convertHtmlToPdf(it) }
        }
    }

    private fun renameFileAsZip(filePath: String?): File? {
        filePath?.let { path ->
            val file = File(path)
            if (file.exists() && file.name.endsWith(MainActivity.CONVERTIBLE_FILE_EXTENSION)) {
                val sFile = File(file.parentFile, ZIP_FILE_NAME)
                if (file.renameTo(sFile)) {
                    showToast(R.string.toast_file_converted, file.name, ZIP_FILE_NAME)
                    return sFile
                } else {
                    showToast(R.string.toast_conversion_failed, file.name)
                }
            }
        } ?: showToast(R.string.toast_invalid_file_selected)
        return null
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
            .setMediaSize(PrintAttributes.MediaSize.ISO_C4) // Set paper size here
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
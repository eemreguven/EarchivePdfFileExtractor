package com.mrguven.e_archivepdffileextractor

import AppFileUtils
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mrguven.e_archivepdffileextractor.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var selectedFile: File? = null

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let { AppFileUtils.getPathFromUri(this, it)?.let { it1 -> selectFile(it1) } }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFileViewNotSelected()
        setButtonClickListeners()
    }

    private fun setButtonClickListeners() {
        binding.apply {
            openFileButton.setOnClickListener { openFilePicker() }
            closeButton.setOnClickListener { clearSelectedFile() }
            convertFileButton.setOnClickListener {
                selectedFile?.let { navigateToConvertActivity(it.path) }
            }
        }
    }

    private fun setFileViewSelected() {
        selectedFile?.let {
            binding.selectedFileView.setImageResource(R.drawable.ic_zip_file)
            binding.selectedFileNameText.text = it.name ?: ""
            binding.openFileButton.visibility = View.GONE
            binding.convertFileButton.visibility = View.VISIBLE
            binding.closeButton.visibility = View.VISIBLE
        }
    }

    private fun setFileViewNotSelected() {
        binding.selectedFileView.setImageResource(R.drawable.ic_blank_file)
        binding.selectedFileNameText.text = getText(R.string.no_selected_file)
        binding.openFileButton.visibility = View.VISIBLE
        binding.convertFileButton.visibility = View.GONE
        binding.closeButton.visibility = View.GONE
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        pickFileLauncher.launch(intent)
    }

    private fun selectFile(filePath: String?) {
        filePath?.let {
            val file = File(it)
            if (file.exists() && file.name.endsWith(CONVERTIBLE_FILE_EXTENSION)) {
                selectedFile = File(file.parentFile, file.name)
                setFileViewSelected()
                showToast(R.string.toast_selected_file, file.name ?: "")
            } else {
                showToast(R.string.toast_invalid_file_selected)
            }
        } ?: showToast(R.string.toast_invalid_file_selected)
    }

    private fun clearSelectedFile() {
        selectedFile = null
        setFileViewNotSelected()
    }

    private fun navigateToConvertActivity(filePath: String?) {
        val intent = Intent(this, ConvertActivity::class.java)
        intent.putExtra(FILE_PATH_KEY, filePath)
        startActivity(intent)
        clearSelectedFile()
    }

    private fun showToast(messageResId: Int, vararg formatArgs: Any) {
        val message = getString(messageResId, *formatArgs)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val CONVERTIBLE_FILE_EXTENSION = ".zip.json"
        const val FILE_PATH_KEY = "filePath"
    }
}
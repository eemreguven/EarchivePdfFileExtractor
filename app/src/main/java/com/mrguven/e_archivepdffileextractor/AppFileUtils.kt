import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object AppFileUtils {
    fun getPathFromUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        var filePath: String? = null

        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)

            cursor?.let {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        val displayName = it.getString(displayNameIndex)
                        val file = File(context.cacheDir, displayName)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                        filePath = file.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return filePath
    }
}

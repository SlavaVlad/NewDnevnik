package well.keepitsimple.dnevnik

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import org.apache.poi.ss.usermodel.Cell
import well.keepitsimple.dnevnik.ui.settings.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


fun <E> ArrayList<E>.addUnique(value: E) {
    if (!this.contains(value)) {
        this.add(value)
    }
}

fun <E> MutableCollection<E>.addUnique(value: E) {
    if (!this.contains(value)) {
        this.add(value)
    }
}

fun <E> MutableCollection<E>.addSwitch(value: E) {
    if (!this.contains(value)) {
        this.add(value)
    } else {
        this.remove(value)
    }
}

fun DocumentSnapshot.getStringList(field: String): List<String> {
    return this[field] as List<String>
}

fun createCheckableChip(ctx: Context, text: String): Chip {
    val c = Chip(ctx)
    c.isCheckable = true
    c.text = text
    return c
}

val ENTER = "\n"


fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun TextInputEditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun <K, V> HashMap<K, V>.putUnique(key: K, value: V) {
    if (!this.containsKey(key)) {
        this[key] = value
    }
}

fun TabLayout.next() {
    selectTab(getTabAt(selectedTabPosition + 1))
}

fun randStr(length: Int): String {
    val alphabet = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
    var res = ""
    repeat(length) {
        res += alphabet[(0..alphabet.length).random()]
    }
    return res
}

fun randCode(length: Int): String {
    val alphabet = "1234567890"
    var res = ""
    repeat(length) {
        res += alphabet[(0..alphabet.length).random()]
    }
    return res
}

fun randClass(): String {
    val letters = "АБВГДЕ"
    var res = ""
    res += (1..11).random().toString()
    res += letters[(letters.indices).random()]
    return res
}

fun buildFirebaseLinkAsync(
    parameters: Map<String, Any>,
    onLinkCompletedListener: ShortLinkCompletedListener
) {
    var uriString = ""
    uriString += "https://keepitsimple.page.link/" // добавили https://.../
    if (parameters.isNotEmpty()) {
        parameters.onEachIndexed { index, entry ->
            uriString += when (index) {
                0 -> "?${entry.key}=${entry.value}"
                else -> "&${entry.key}=${entry.value}"
            }
        }
    }

    Firebase.dynamicLinks.shortLinkAsync {
        link = Uri.parse(uriString)
        domainUriPrefix = "https://keepitsimple.page.link/"
        Log.d(TAG, "script: ${this.longLink}")
    }.addOnSuccessListener {
        onLinkCompletedListener.onCompleted(it)
    }.addOnFailureListener { e ->
        Log.e(TAG, "buildFirebaseLinkAsync: failed $e")
    }.addOnCanceledListener {
        Log.e(TAG, "buildFirebaseLinkAsync: cancelled")
    }
}

fun downloadFileFromRawFolder(
    resId: Int,
    fileName: String,
    folderName: String,
    activity: Activity
): File? {
    with(activity) {
        try {
            val input: InputStream = resources.openRawResource(
                resId
            ) // use only file name here, don't use extension
            val fileWithinMyDir =
                File(checkFolder(activity, folderName), fileName) //Getting a file within the dir.
            Log.e("FILEPATH", "fileWithinMyDir $fileWithinMyDir")
            val out = FileOutputStream(fileWithinMyDir)
            val buff = ByteArray(1024 * 1024 * 2) //2MB file
            var read = 0
            try {
                while (input.read(buff).also { read = it } > 0) {
                    out.write(buff, 0, read)
                }
            } finally {
                input.close()
                out.close()
            }
            Log.d(TAG, "Download Done ")
            return fileWithinMyDir
        } catch (e: IOException) {
            Log.e(TAG, "Download Failed ${e.message}")
            e.printStackTrace()
        }
    }
    return null
}

private fun checkFolder(activity: Activity, folderName: String): File {
    val path: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        activity.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + folderName
    } else {
        Environment.getExternalStorageDirectory().toString() + folderName
    }
    val dir = File(path)
    var isDirectoryCreated = dir.exists()
    if (!isDirectoryCreated) {
        isDirectoryCreated = dir.mkdir()
        Log.d("Folder", "Created = $isDirectoryCreated")
    }
    Log.d("Folder", "Created ? $isDirectoryCreated")
    return dir
}

fun Cell.getStringOrNull(): String? {
    return if (stringCellValue != null) {
        stringCellValue
    } else {
        null
    }
}
fun Cell.getDoubleOrNull(): Double? {
    return if (stringCellValue != null) {
        numericCellValue
    } else {
        null
    }
}

fun Double?.toIntOrNull(): Int? {
    return if (this != null) {
        null
    } else {
        this!!.toInt()
    }
}

fun Cell.getValue(): Any? {
    var returnIt: Any? = null
    try {
        returnIt = numericCellValue
    } catch (e: Exception) {
        try {
            returnIt = stringCellValue
        } catch (e: Exception) {
            Log.e(TAG, "getValue: cannot get value string/numeric from Cell")
        }
    }
    return returnIt
}
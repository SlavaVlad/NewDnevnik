package well.keepitsimple.dnevnik

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot

fun <E> ArrayList<E>.addUnique(value: E) {
    if (! this.contains(value)) {
        this.add(value)
    }
}
fun <E> MutableCollection<E>.addUnique(value: E) {
    if (! this.contains(value)) {
        this.add(value)
    }
}

fun <E> MutableCollection<E>.addSwitch(value: E) {
    if (! this.contains(value)) {
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
    if (! this.containsKey(key)) {
        this[key] = value
    }
}

fun TabLayout.next(){
    selectTab(getTabAt(selectedTabPosition + 1))
}

fun randStr(length: Int): String {
    val alphabet = "qwertyuiopasdfghjklzxcvbnm"
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
    res += letters[(0..letters.length-1).random()]
    return res
}
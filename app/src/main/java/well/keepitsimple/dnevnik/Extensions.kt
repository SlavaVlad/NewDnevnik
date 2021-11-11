package well.keepitsimple.dnevnik

import android.content.Context
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.DocumentSnapshot

fun <E> ArrayList<E>.addUnique(value: E){
    if (!this.contains(value)) {
        this.add(value)
    }
}
fun <E> ArrayList<E>.addSwitch(value: E){
    if (!this.contains(value)) {
        this.add(value)
    } else {
        this.remove(value)
    }
}

fun DocumentSnapshot.getListOfStrings(field: String): List<String> {
    return this[field] as List<String>
}

fun createCheckableChip(ctx: Context, text: String): Chip {
    val c = Chip(ctx)
    c.isCheckable = true
    c.text = text
    return c
}
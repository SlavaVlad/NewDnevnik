package well.keepitsimple.dnevnik

import com.google.firebase.firestore.DocumentSnapshot

fun <E> ArrayList<E>.addUnique(value: E){
    if (this.contains(value)) {
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

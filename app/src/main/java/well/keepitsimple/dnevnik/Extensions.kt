package well.keepitsimple.dnevnik

import java.lang.reflect.Array

fun <E> ArrayList<E>.addUnique(value: E){
    if (this.contains(value)) {
        this.add(value)
    }
}

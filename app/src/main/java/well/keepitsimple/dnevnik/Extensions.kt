package well.keepitsimple.dnevnik

fun <E> ArrayList<E>.addUnique(value: E){
    if (this.contains(value)) {
        this.add(value)
    }
}

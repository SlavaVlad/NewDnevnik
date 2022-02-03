package well.keepitsimple.dnevnik

interface OnCompletedListener {
    fun onCompleted(success: Boolean, msg: String){}
}
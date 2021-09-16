package well.keepitsimple.dnevnik.ui.timetables

data class Lesson(
    val cab: Long,
    val name: String,
    val startAt: String,
    val endAt: String,
    val day: Long,
)
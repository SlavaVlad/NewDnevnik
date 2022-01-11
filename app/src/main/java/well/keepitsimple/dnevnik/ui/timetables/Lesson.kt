package well.keepitsimple.dnevnik.ui.timetables

data class Lesson (
    val cab: String,
    val name: String? = null,
    val time: LessonsTime,
    val day: Long,
    var groupId: String? = null,
)
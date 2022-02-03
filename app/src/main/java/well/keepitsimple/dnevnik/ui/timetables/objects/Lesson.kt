package well.keepitsimple.dnevnik.ui.timetables.objects

data class Lesson(
    val index: Long,
    val cab: String,
    val name: String,
    val teacherName: String?,
    val time: LessonsTime?,
    val day: Long,
    var groupId: String?,
)

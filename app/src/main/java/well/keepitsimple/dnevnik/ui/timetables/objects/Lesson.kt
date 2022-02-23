package well.keepitsimple.dnevnik.ui.timetables.objects

data class Lesson(
    val index: Int,
    val cab: String?,
    val name: String,
    val teacher: String?,
    val time: LessonsTime?,
    val day: Int,
) {
    var tag: String? = null
    var groupId: String? = null
    fun isGroup (): Boolean {
        return groupId != null
    }
}

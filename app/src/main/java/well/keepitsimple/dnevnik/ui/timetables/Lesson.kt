package well.keepitsimple.dnevnik.ui.timetables

data class Lesson(
    var cab: Long,
    var name: String,
    var startAt: String,
    var endAt: String,
    var day: Long,
)
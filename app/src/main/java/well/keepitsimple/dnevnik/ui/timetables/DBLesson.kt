package well.keepitsimple.dnevnik.ui.timetables

data class DBLesson (
    var name: String? = null,
    var cab: String? = null, // на всякий случай строка, ибо существуют кабинеты 506а, 506г и так далее.
    var index: Long? = null, // если его нет, то берём из массива, а если есть, то считаем корректировку по времени
    var groupId: String? = null,
)
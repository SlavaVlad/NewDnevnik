package well.keepitsimple.dnevnik.ui.timetables.objects

import java.time.LocalTime
import java.util.*
import java.util.Calendar.DAY_OF_WEEK

class Timetable(_lessons: MutableList<Lesson>) {

    val lessons = _lessons
    var lessonsByDays: MutableList<List<Lesson>> = mutableListOf()
    var count = 0
    var teachers = mutableListOf<String>()
    val cabs = mutableListOf<String>()

    init {

        count = lessons.size
        lessons.forEach {
            if (it.teacher!=null){
                teachers.add(it.teacher)
            }
            it.cab?.let { filter -> cabs.add(filter) }
        }
        teachers.distinct()
        cabs.distinct()

        val dows = lessons.maxOf { lesson ->
            lesson.day
        }
        repeat(dows){ i ->
            lessonsByDays.add(lessons.filter{ it.day == i+1})
        }

    }

    fun getCurrentLesson(): Lesson? {
        val todayLessons = lessons.filter { it.day == Calendar.getInstance(TimeZone.getDefault()).get(DAY_OF_WEEK) }

        try {
            todayLessons.forEach {

                val from = LocalTime.of(
                    "${it.time!!.startAt!![0]}${it.time.startAt!![1]}".toInt(),
                    "${it.time.startAt[3]}${it.time.startAt[4]}".toInt()
                )
                val to = LocalTime.of(
                    "${it.time.endAt!![0]}${it.time.endAt[1]}".toInt(),
                    "${it.time.endAt[3]}${it.time.endAt[4]}".toInt()
                ).plusMinutes(45)

                if (TimePeriod(from, to).isTimeIn(LocalTime.now())){
                    return it
                }

            }
        } catch (e: NullPointerException) {
            throw Exception("TimeLesson is missing")
        }
        return null
    }

}
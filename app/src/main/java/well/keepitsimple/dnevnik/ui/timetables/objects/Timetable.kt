package well.keepitsimple.dnevnik.ui.timetables.objects

import java.time.LocalTime
import java.util.*
import java.util.Calendar.DAY_OF_WEEK

class Timetable(_lessons: ArrayList<Lesson>) {

    val lessons = _lessons
    var count = 0
    var teachers = mutableListOf<String>()
    val cabs = mutableListOf<String>()

    init {
        count = lessons.size
        lessons.forEach {
            if (it.teacherName!=null){
                teachers.add(it.teacherName)
            }
            cabs.add(it.cab)
        }
        teachers.distinct()
        cabs.distinct()
    }

    fun getCurrentLesson(): Lesson? {
        val todayLessons = lessons.filter { it.day == Calendar.getInstance(TimeZone.getDefault()).get(DAY_OF_WEEK).toLong() }

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
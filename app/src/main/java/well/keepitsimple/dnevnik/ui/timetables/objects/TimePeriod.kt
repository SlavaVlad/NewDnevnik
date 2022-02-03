package well.keepitsimple.dnevnik.ui.timetables.objects

import java.time.LocalTime


data class TimePeriod(val from: LocalTime, val to: LocalTime) {
    fun isTimeIn(time: LocalTime) = time > from && time < to
}

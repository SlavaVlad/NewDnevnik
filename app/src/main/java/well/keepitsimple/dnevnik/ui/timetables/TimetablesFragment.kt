package well.keepitsimple.dnevnik.ui.timetables

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.core.view.get
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.*
import well.keepitsimple.dnevnik.ui.timetables.lessons.LessonsAdapter
import kotlin.collections.ArrayList


class TimetablesFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var list: ListView
    lateinit var tabs: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timetables, container, false)

        list = view.findViewById(R.id.list)
        tabs = view.findViewById(R.id.tab)

        setup()

        return view
    }

    data class Lesson(
        val cab: Long,
        val name: String,
        val timeIndex: Long,
        val type: String,
        val startAt: String,
        val endAt: String,
        val day: Long,
        val day_name: String,
    )

    private fun setup() {

        val list_lessons = ArrayList<Lesson>()
        list_lessons.clear()
        // запрос документов расписания
        db.collection("lessonstime").document("LxTrsAIg81E96zMSg0SL").get().addOnSuccessListener { lesson_time -> // расписание звонков
            db.collection("lessons").get().addOnSuccessListener { lesson_query ->
                repeat(lesson_query.size()) {
                    val lesson = lesson_query.documents[it]
                    repeat(lesson.getLong("lessonsCount")!!.toInt()) { loop ->
                        val loopindex: Int = loop + 1
                        val day: String = when (lesson.getLong("day")!!.toInt()) {
                            1 -> "ПН"
                            2 -> "ВТ"
                            3 -> "СР"
                            4 -> "ЧТ"
                            5 -> "ПТ"
                            6 -> "СБ"
                            else -> "exception"
                        }
                        list_lessons.add(Lesson(
                            cab = lesson.getLong("${loopindex}_cab")!!,
                            name = lesson.getString("${loopindex}_name")!!,
                            timeIndex = lesson.getLong("${loopindex}_timeIndex")!!,
                            type = lesson.getString("${loopindex}_type")!!,
                            startAt = lesson_time.getString("${loopindex}_startAt")!!,
                            endAt = lesson_time.getString("${loopindex}_endAt")!!,
                            day = lesson.getLong("day")!!,
                            day_name = day
                        ))
                    }
                }

                setList("ПН", list_lessons)
                    tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
                        override fun onTabSelected(tab: TabLayout.Tab?) {
                            setList(tab!!.text.toString(), list_lessons)
                        }
                        override fun onTabUnselected(tab: TabLayout.Tab?) {
                        }
                        override fun onTabReselected(tab: TabLayout.Tab?) {
                        }
                    })

                }
            }
        }

    private fun setList(dayOfWeek: String, lr: ArrayList<Lesson>) {

        val lessons: ArrayList<Lesson> = ArrayList()
        lessons.clear()

        for (i in 0 until lr.size) {
            if (lr[i].day_name == dayOfWeek) {
                lessons.add(lr[i])
            }
        }

        val lessonsAdapter = LessonsAdapter(requireActivity().baseContext, R.layout.lesson_item, lr)

        list.adapter = lessonsAdapter

    }
}
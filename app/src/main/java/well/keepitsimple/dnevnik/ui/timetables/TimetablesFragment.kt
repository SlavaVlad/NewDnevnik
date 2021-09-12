package well.keepitsimple.dnevnik.ui.timetables

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.*
import kotlin.collections.ArrayList


class TimetablesFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var vp_lessons: ViewPager
    lateinit var tl_lessons: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timetables, container, false)

        vp_lessons = view.findViewById(R.id.vp_lessons)
        tl_lessons = view.findViewById(R.id.tl_lessons)

        tl_lessons.setupWithViewPager(vp_lessons)

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
        // запрос документов расписания
        db.collection("lessonstime").document("LxTrsAIg81E96zMSg0SL").get()
            .addOnSuccessListener { lesson_time -> // расписание звонков
                db.collection("lessons").document("REmF4KZTCwxXo77Cb7KO").get()
                    .addOnSuccessListener { lesson ->
                        val list_lessons = ArrayList<Lesson>()
                        val adapter = LessonsAdapter(requireActivity().supportFragmentManager, 0)

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

                        val args = Bundle()
                        val item = TimetablesItem()
                        repeat(list_lessons.size-1) { loopindex ->
                            args.putString("name", list_lessons[loopindex].name)
                            args.putString("type", list_lessons[loopindex].type)
                            args.putInt("cab", list_lessons[loopindex].cab.toInt())
                            args.putInt("timeIndex", list_lessons[loopindex].timeIndex.toInt())
                            args.putString("startAt", list_lessons[loopindex].startAt)
                            args.putString("endAt", list_lessons[loopindex].endAt)
                            args.putInt("day", list_lessons[loopindex].day.toInt())
                            args.putString("day_name", list_lessons[loopindex].day_name)
                            item.arguments = args
                            adapter.addFragment(item, list_lessons[loopindex].day_name)
                        }
                        vp_lessons.adapter = adapter

                    }
            }
    }


}
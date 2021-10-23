package well.keepitsimple.dnevnik.ui.timetables

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import well.keepitsimple.dnevnik.*
import well.keepitsimple.dnevnik.ui.timetables.lessons.LessonsAdapter
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext


class TimetablesFragment : Fragment(), CoroutineScope {
    val db = FirebaseFirestore.getInstance()
    lateinit var list: ListView
    lateinit var tabs: TabLayout
    lateinit var ctx: Activity
    val lessons = ArrayList<Lesson>()
    var gactivity: MainActivity? = null

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timetables, container, false)

        list = view.findViewById(R.id.list)
        tabs = view.findViewById(R.id.tab)
        gactivity = activity as MainActivity?

        return view
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        ctx = activity
    }

    private fun setup(list_lessons: ArrayList<Lesson>) {

        setList(0, list_lessons) // Вызываем создание расписания на сегодняшний день TODO: Определение дня по системе автоматически

                tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{ // выбрали расписание на другой день
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        setList(tab!!.position, list_lessons)
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }
                })
                }

    private fun setList(dayOfWeek: Int, lr: ArrayList<Lesson>) {

        lessons.clear()

        repeat(lr.size) {
            if (lr[it].day.toInt() == dayOfWeek) {
                lessons.add(lr[it])
            }
        }

        val lessonsAdapter = LessonsAdapter(ctx.baseContext, R.layout.lesson_item, lessons)

        list.adapter = lessonsAdapter

    }
}
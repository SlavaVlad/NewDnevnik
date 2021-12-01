package well.keepitsimple.dnevnik.ui.timetables

import android.app.Activity
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.timetables.lessons.LessonsAdapter
import java.util.Calendar.DAY_OF_WEEK
import kotlin.coroutines.CoroutineContext

const val TAG = "Timetables"

class TimetablesFragment : Fragment(), CoroutineScope {
    val db = FirebaseFirestore.getInstance()
    lateinit var list: ListView
    lateinit var tabs: TabLayout
    lateinit var ctx: Activity
    val lessons = ArrayList<Lesson>()
    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timetables, container, false)

        list = view.findViewById(R.id.vp_parents)
        tabs = view.findViewById(R.id.tabs_dow)

        setup(act.list_lessons)

        return view
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        ctx = activity
    }

    private fun setup(list_lessons: ArrayList<Lesson>) {

        tabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener { // выбрали расписание на другой день
            override fun onTabSelected(tab: TabLayout.Tab?) {
                setList(tab !!.position, list_lessons)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val dow = calendar.get(DAY_OF_WEEK)-1
        Log.d(TAG, "setup: $dow")

        if (dow <= tabs.tabCount) {
            tabs.selectTab(tabs.getTabAt(dow-1))
            setList(dow-1, list_lessons)
        }
    }

    private fun setList(dayOfWeek: Int, lr: ArrayList<Lesson>) {

        lessons.clear()

        repeat(lr.size) {
            if (lr[it].day.toInt()-1 == dayOfWeek) {
                lessons.add(lr[it])
            }
        }

        val lessonsAdapter = LessonsAdapter(ctx.baseContext, R.layout.lesson_item, lessons)

        list.adapter = lessonsAdapter
    }
}

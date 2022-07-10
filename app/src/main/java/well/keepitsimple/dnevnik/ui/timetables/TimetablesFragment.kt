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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.UserDataSetEvent
import well.keepitsimple.dnevnik.ui.timetables.lessons.LessonsAdapter
import well.keepitsimple.dnevnik.ui.timetables.objects.Lesson
import java.util.Calendar.DAY_OF_WEEK
import kotlin.coroutines.CoroutineContext

const val TAG = "Timetables"

class TimetablesFragment : Fragment(), CoroutineScope {

    lateinit var list: ListView
    lateinit var tabs: TabLayout
    lateinit var ctx: Activity
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

        EventBus.getDefault().register(this)

        list = view.findViewById(R.id.vp_parents)
        tabs = view.findViewById(R.id.tabs_dow)

        return view
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        ctx = activity
    }

    override fun onStart() {
        super.onStart()
        if (act.timetable != null) {
            setup(act.timetable!!.lessons)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMessageEvent(event: UserDataSetEvent?) {
        setup(act.timetable!!.lessons)
    }

    private fun setup(listLessons: List<Lesson>) {

        tabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener { // выбрали расписание на другой день
            override fun onTabSelected(tab: TabLayout.Tab?) {
                setList(tab !!.position, listLessons)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val dow = calendar.get(DAY_OF_WEEK)
        Log.d(TAG, "setup: $dow")

        if (dow-1 <= tabs.tabCount) {
            tabs.selectTab(tabs.getTabAt(dow-2))
            setList(dow, listLessons)
        } else {
            tabs.selectTab(tabs.getTabAt(dow-2))
            setList(0, listLessons)
        }
    }

    private fun setList(dayOfWeek: Int, lr: List<Lesson>) {

        val lessons = mutableListOf<Lesson>()

        lr.forEach {
            if (it.day-1 == dayOfWeek-2) {
                lessons.add(it)
            }
        }

        val lessonsAdapter = LessonsAdapter(ctx.baseContext, R.layout.lesson_item, lessons)

        list.adapter = lessonsAdapter
    }
}

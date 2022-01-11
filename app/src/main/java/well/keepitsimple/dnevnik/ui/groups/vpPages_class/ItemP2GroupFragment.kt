package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.default.SlideAdapter
import well.keepitsimple.dnevnik.next
import well.keepitsimple.dnevnik.ui.groups.CreateClass
import kotlin.coroutines.CoroutineContext

class ItemP2GroupFragment : Fragment(), CoroutineScope {

    val db = FirebaseFirestore.getInstance()

    val TAG = "ItemP2GroupFragment"

    val act: MainActivity by lazy {
        activity as MainActivity
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    lateinit var tabs: TabLayout
    lateinit var vp_timetable: ViewPager2
    lateinit var btn_next: Button
    lateinit var btn_current: Button

    // id документа родителя
    // название группы
    // пользователи \ код доступа
    // права, которые группа даёт пользователям
    // админы и их права
    // тип группы пишется автоматом в зависимости от родителя
    // ид расписания

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_item_p2_create_timetable_group, container, false)

        Log.d(TAG, "onCreateView: CREATE_VIEW")

        tabs = view.findViewById(R.id.tabs_dow)
        vp_timetable = view.findViewById(R.id.vp_timetable)
        btn_next = view.findViewById(R.id.btn_next_dow)
        btn_current = view.findViewById(R.id.btn_current)

        vp_timetable.adapter = SlideAdapter(
            this, mutableListOf(
                FragmentCreateTimetablePage(),
                FragmentCreateTimetablePage(),
                FragmentCreateTimetablePage(),

                FragmentCreateTimetablePage(),
                FragmentCreateTimetablePage(),
                FragmentCreateTimetablePage(),
            )
        )

        vp_timetable.isSaveEnabled = true

        tabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                vp_timetable.setCurrentItem(tab!!.position, true)
                btn_current.text = "Выбрано: ${tabs.getTabAt(tabs.selectedTabPosition)!!.text}"

                val tabIndex = tabs.selectedTabPosition

                if (tabIndex != 5 && tabIndex != 0) { // вт-пт
                    btn_next.text = "К ${tabs.getTabAt(tabIndex + 1)!!.text}"
                } else {
                    if (tabIndex == 0) { // 0
                        btn_next.text = "К ${tabs.getTabAt(tabIndex + 1)!!.text}"
                    } else { // 5
                        btn_next.text = "Завершить"
                    }
                }

                Log.d(TAG, "onTabSelected: $tabIndex")

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btn_next.setOnClickListener {

            val item = ((vp_timetable.adapter as SlideAdapter).getItem(vp_timetable.currentItem) as FragmentCreateTimetablePage)
            val dryData = item.lessons

            val lessons = mutableListOf<HashMap<String, String>>()

            //(item.lessons[it][0] as TextInputEditText).text.toString() - имя предмета
            //(item.lessons[it][0] as TextInputEditText).text.toString() - кабинет
            //(item.lessons[it][0] as TextInputEditText).text.toString() - индекс группы, если не "0"

            var count = 0
            repeat(item.lessonsCount) {
                if ((dryData[it][0] as TextInputEditText).text!!.isNotEmpty()) {
                    count++
                }
            } // Посчитали кол-во уроков

            repeat(count) {
                val hm = hashMapOf(
                    "name" to (item.lessons[it][0] as TextInputEditText).text.toString(),
                    "cab" to (item.lessons[it][1] as TextInputEditText).text.toString(),
                )
                if ((item.lessons[it][2] as TextView).text.toString().toInt() != 0) {
                    hm["groupId"] = (item.lessons[it][2] as TextView).text.toString()
                }
                lessons.add(hm)
            } // сложили уроки как Map's в лист lessons

            val payload = hashMapOf(
                "dow" to vp_timetable.currentItem + 1,
                "offset" to (item.lessons[0][3] as TextView).text.toString().toInt(),
                "lessons" to lessons
            )

            launch { (requireParentFragment() as CreateClass).addDay(payload, count) }

            tabs.next()

        }

        return view
    }

}
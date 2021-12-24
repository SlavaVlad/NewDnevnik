package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.default.SlideAdapter
import well.keepitsimple.dnevnik.next
import well.keepitsimple.dnevnik.ui.groups.CreateClass

class ItemP2GroupFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()

    val TAG = "ItemP2GroupFragment"

    val act: MainActivity by lazy {
        activity as MainActivity
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

        vp_timetable.adapter = SlideAdapter(this, mutableListOf(

            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),

            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),

        ))

        vp_timetable.isSaveEnabled = true

        tabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                vp_timetable.setCurrentItem(tab !!.position, true)
                btn_current.text = "Выбрано: ${tabs.getTabAt(tabs.selectedTabPosition) !!.text}"

                val tabIndex = tabs.selectedTabPosition

                if (tabIndex != 5 && tabIndex != 0) { // вт-пт
                    btn_next.text = "К ${tabs.getTabAt(tabIndex + 1) !!.text}"
                } else {
                    if (tabIndex == 0) { // 0
                        btn_next.text = "К ${tabs.getTabAt(tabIndex + 1) !!.text}"
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

            val payload = hashMapOf<String, Any>(
                "day" to vp_timetable.currentItem + 1,
                "timeOffset" to item.etOffset.text.toString().toInt()
            )

            var lessonsCount = 0
            repeat(item.lessonsCount){
                if (dryData[it][0].text!!.isNotEmpty()){
                    lessonsCount++
                }
            }
            payload["lessonsCount"] = lessonsCount
            repeat(lessonsCount){
                payload["${it + 1}_name"] = item.lessons[it][0].text.toString()
                payload["${it + 1}_cab"] = item.lessons[it][1].text.toString().toInt()
            }

            (requireParentFragment() as CreateClass).addDay(payload)

            tabs.next()

        }

        return view
    }

}
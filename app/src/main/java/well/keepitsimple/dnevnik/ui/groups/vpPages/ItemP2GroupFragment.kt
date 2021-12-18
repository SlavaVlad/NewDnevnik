package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.default.SlideAdapter
import well.keepitsimple.dnevnik.ui.groups.CreateGroup

class Parser {
    
}

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
    lateinit var fab_create_lesson: FloatingActionButton

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
        fab_create_lesson = view.findViewById(R.id.fab_create_lesson)

        vp_timetable.adapter = SlideAdapter(this, mutableListOf(
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
            FragmentCreateTimetablePage(),
        ))

        vp_timetable.isSaveEnabled = true

        fab_create_lesson.setOnClickListener {
            val recycler =
                ((vp_timetable.adapter as SlideAdapter).createFragment(tabs.selectedTabPosition) as FragmentCreateTimetablePage).rv_timetable
            (recycler.adapter as TimetableCreateAdapter).insertItem()
            recycler.scrollToPosition((recycler.adapter as TimetableCreateAdapter).itemCount)
        }

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
            tabs.selectTab(tabs.getTabAt(tabs.selectedTabPosition + 1))

            if (tabs.selectedTabPosition == 5) {
                val pf = requireParentFragment() as CreateGroup
                val timetableVisual = vp_timetable.adapter as SlideAdapter

                var day = 1
                repeat(timetableVisual.itemCount) { fragIndex ->
                    val lessonsToFirestore = hashMapOf<String, Any>()
                    val fragment =
                        (timetableVisual.getItem(fragIndex) as FragmentCreateTimetablePage)

                    var index = 0
                    (fragment.rv_timetable.adapter as TimetableCreateAdapter).itemsHm.forEach {
                        if (it["name"] !!.text !!.isNotEmpty() && it["cab"] !!.text !!.isNotEmpty()) {
                            lessonsToFirestore["${index}_name"] = it["name"] !!.text.toString()
                            lessonsToFirestore["${index}_cab"] = it["cab"] !!.text.toString().toInt()
                        }
                        index ++
                    }
                    lessonsToFirestore["lessonsCount"] = index
                    lessonsToFirestore["day"] = day
                    lessonsToFirestore["timeOffset"] = fragment.et_offset.text.toString().toInt()
                    day ++
                    pf.t.add(lessonsToFirestore)
                }

                (pf.vpCreateGroup.adapter as SlideAdapter).insertItem(ItemP3GroupFragment())
                pf.vpCreateGroup.currentItem = 2
            }

        }

        return view
    }

}
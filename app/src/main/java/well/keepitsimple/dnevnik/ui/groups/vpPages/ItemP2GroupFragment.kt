package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R

class ItemP2GroupFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()

    val TAG = "ItemP2GroupFragment"

    val act: MainActivity by lazy {
        activity as MainActivity
    }



    var lessonsCount = 0

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
        val view =  inflater.inflate(R.layout.fragment_item_p2_create_timetable_group, container, false)

        val tabs = view.findViewById<TabLayout>(R.id.tabs_dow)

        tabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addLesson()
    }

    private fun addLesson() {
        val layout: LinearLayout = requireView().findViewById(R.id.lay_timetables)
        val item =
            LayoutInflater.from(requireActivity()).inflate(R.layout.item_timetable, null, false)

        val et_cab = item.findViewById<TextInputEditText>(R.id.et_lesson_cab)
        val et_name = item.findViewById<TextInputEditText>(R.id.et_lesson_name)

        item.findViewById<AppCompatImageView>(R.id.imBtn_delete).setOnClickListener {
            if (layout.size > 1) {
                layout.removeView(item)
            }
        }

        et_cab.tag = lessonsCount

        et_cab.setImeActionLabel("Add", EditorInfo.IME_ACTION_NEXT)

        et_cab.setOnEditorActionListener { textView, i, keyEvent ->
            addLesson()
            true
        }

        layout.addView(item)

        et_name.requestFocus()

        lessonsCount ++
    }
}
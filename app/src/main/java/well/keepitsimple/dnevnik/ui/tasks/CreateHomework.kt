package well.keepitsimple.dnevnik.ui.tasks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

class CreateHomework : Fragment(), CoroutineScope {

    val db = FirebaseFirestore.getInstance()

    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    lateinit var cg_types: ChipGroup
    lateinit var cg_subjects: ChipGroup
    lateinit var cg_targets: ChipGroup
    lateinit var btn_complete: Button
    lateinit var et_text: EditText
    lateinit var calendar: CalendarView

    var gDate = Date()

    private var textLength = 0

    private val data = hashMapOf<String, Any>()

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
        val view = inflater.inflate(R.layout.fragment_create_homework, container, false)

        cg_types = view.findViewById(R.id.cg_type)
        cg_subjects = view.findViewById(R.id.cg_subject)
        cg_targets = view.findViewById(R.id.cg_target)
        btn_complete = view.findViewById(R.id.btn_complete)
        et_text = view.findViewById(R.id.et_text)
        calendar = view.findViewById(R.id.calendar)
        calendar.firstDayOfWeek = 2
        calendar.minDate = System.currentTimeMillis() + DAY_S * 1000

        btn_complete.setOnClickListener {
            btn_complete.isEnabled = false
            send()
        }

        calendar.setOnDateChangeListener { calendarView, y, m, d ->
            gDate = Date(y-1900,m,d)
            Log.d(TAG, "calendar: $d ~ $m ~ $y")
            Log.d(TAG, "calendar: ${gDate.toLocaleString()}")
        }

        loadAllChips()

        et_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textLength = s.length
            }
        })

        return view
    }

    private fun check(): Boolean {
        return textLength > 2 && cg_subjects.checkedChipIds != emptyList<Int>() && cg_targets.checkedChipIds != emptyList<Int>() && cg_types.checkedChipIds != emptyList<Int>()
    }

    private fun loadAllChips() {
        loadTypes()
        loadTodaySubjects()
        loadTargets()
        btn_complete.isEnabled = true
    }

    private fun loadTargets() {

        val chip = createCheckableChip(requireContext(), act.user.getGroupByType("class").name !!)
        cg_targets.addView(chip)
    }

    private fun loadTypes() {
        val types = arrayOf("Д/з", "Работа на оценку", "Тест")
        types.forEach {
            cg_types.addView(createCheckableChip(requireContext(), it))
        }
    }

    private fun loadTodaySubjects() {

        val sysCalendar = Calendar.getInstance(TimeZone.getDefault())

        val uniqueLessons = ArrayList<String>()

        act.list_lessons.forEach { lesson ->
            if (lesson.day.toInt() == sysCalendar.get(Calendar.DAY_OF_WEEK) - 1 && ! uniqueLessons.contains(
                    lesson.name
                )
            ) {
                val c = createCheckableChip(requireContext(), lesson.name)
                cg_subjects.addView(c)
                c.setOnClickListener { v ->
                    val cc = v as Chip
                    calendar.date = act.getNextLesson(cc.text.toString()) * 1000
                    gDate = Date(act.getNextLesson(cc.text.toString()) * 1000)
                    Log.d(TAG, "calendar: ${gDate.toLocaleString()}")
                    Log.d(TAG, "calendar: ${calendar.date}")
                }
                uniqueLessons.add(lesson.name)
            }
        }

        val c = createCheckableChip(requireContext(), "Показать всё")
        cg_subjects.addView(c)
        c.setOnClickListener {
            cg_subjects.removeAllViews()
            it.visibility = View.GONE
            loadAllSubjects()
        }
    }

    private fun loadAllSubjects() {

        val names = arrayListOf<String>()

        act.list_lessons.forEach {
            names.addUnique(it.name)
        }

        names.forEach {
            val c = createCheckableChip(requireContext(), it)
            cg_subjects.addView(c)
            c.setOnClickListener {
                val cc = it as Chip
                calendar.date = act.getNextLesson(cc.text.toString()) * 1000
                gDate = Date(act.getNextLesson(cc.text.toString()) * 1000)
                Log.d(TAG, "calendar: ${gDate.toLocaleString()}")
                Log.d(TAG, "calendar: ${calendar.date}")
            }
        }
    }

    private fun send() {
        if (check()) {

            data["text"] = et_text.text.toString()
            data["subject"] = requireView().findViewById<Chip>(cg_subjects.checkedChipId).text.toString()
            data["type"] = requireView().findViewById<Chip>(cg_types.checkedChipId).text.toString()
            data["completed"] = emptyMap<String, Any>()
            data["owner"] = act.user.uid !!
            data["deadline"] = Timestamp(gDate)
            Log.d(TAG, "loadTodaySubjects: ${calendar.date}")
            Log.d(TAG, "send: $gDate; deadline: ${data["deadline"]}")

            db.collection("groups")
                .document(act.user.getGroupByType("school").id !!)
                .collection("groups")
                .document(act.user.getGroupByType("class").id !!)
                .collection("tasks")
                .document()
                .set(data)
                .addOnSuccessListener {
                    val trans: FragmentTransaction = requireFragmentManager()
                        .beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .addToBackStack(null)
                    trans.replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    trans.commit()
                    Snackbar.make(requireView(),
                        "Задание создано успешно",
                        Snackbar.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    btn_complete.isEnabled = true
                    Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "send: $it")
                }
        } else {
            act.alert("Ошибка даты",
                "Задание не может быть задано на сегодняшний день",
                "send()")
            btn_complete.isEnabled = true
        }

    }
}

package well.keepitsimple.dnevnik.ui.tasks

import android.os.Bundle
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.addUnique
import well.keepitsimple.dnevnik.createCheckableChip
import java.util.*
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

class EditHomework : Fragment(), CoroutineScope {

    val db = FirebaseFirestore.getInstance()

    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    val doc: DocumentSnapshot by lazy {
        act.toEdit !! // fixme убрать этот изврат, использовать data bus
    }

    val chips = hashMapOf<String, Chip>()

    lateinit var cg_types: ChipGroup
    lateinit var cg_subjects: ChipGroup
    lateinit var cg_targets: ChipGroup
    lateinit var btn_complete: Button
    lateinit var et_text: EditText
    lateinit var calendar: CalendarView

    var gd = 0
    var gm = 0
    var gy = 0

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

        btn_complete.setOnClickListener {
            btn_complete.isEnabled = false
            update()
        }

        calendar.setOnDateChangeListener { calendarView, d, m, y ->
            gd=d
            gm=m
            gy=y-1970
        }



        et_text.setText(doc.getString("text"))

        loadAllChips()
        checkChipsToEdit()

        calendar.date = doc.getTimestamp("deadline") !!.toDate().toInstant().toEpochMilli()

        gy = doc.getTimestamp("deadline") !!.toDate().year
        gm = doc.getTimestamp("deadline") !!.toDate().month
        gd = doc.getTimestamp("deadline") !!.toDate().day

        return view
    }

    private fun loadAllChips() {
        loadTypes()
        loadAllSubjects()
        loadTargets()
        btn_complete.isEnabled = true
    }

    private fun loadTargets() {

        val c = createCheckableChip(requireContext(), act.user.getGroupByType("class").name !!)
        c.isChecked = true
        chips[c.text.toString()] = c
        cg_targets.addView(c)
    }

    private fun loadTypes() {
        val types = arrayOf("Д/з", "Работа на оценку", "Тест")
        types.forEach {
            val c = createCheckableChip(requireContext(), it)
            chips[c.text.toString()] = c
            cg_types.addView(c)
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
            chips[c.text.toString()] = c
            c.setOnClickListener {
                val cc = it as Chip
                calendar.date =
                    act.getNextLesson(cc.text.toString()) * 1000
                gd = Date(act.getNextLesson(cc.text.toString()) * 1000).day
                gm = Date(act.getNextLesson(cc.text.toString()) * 1000).month
                gy = Date(act.getNextLesson(cc.text.toString()) * 1000).year
            }
        }
    }

    private fun checkChipsToEdit() {
        cg_subjects.check(chips[doc.getString("subject")] !!.id)
        cg_types.check(chips[doc.getString("type")] !!.id)
    }

    private fun update() {
        data["text"] = et_text.text.toString()
        data["subject"] =
            requireView().findViewById<Chip>(cg_subjects.checkedChipId).text.toString()
        data["type"] = requireView().findViewById<Chip>(cg_types.checkedChipId).text.toString()
        data["completed"] = emptyMap<String, Any>()
        data["owner"] = act.user.uid !!
        data["${System.currentTimeMillis()}"] = doc // для истории версий

        data["deadline"] = Timestamp(java.sql.Date(gy, gm+1, gd))

        db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id !!)
            .collection("tasks")
            .document(doc.id)
            .update(data)
            .addOnSuccessListener {
                val trans: FragmentTransaction = requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .addToBackStack(null)
                trans.replace(R.id.nav_host_fragment_content_main, TasksFragment())
                trans.commit()
                Snackbar.make(requireView(), "Задание обновлено успешно", Snackbar.LENGTH_LONG)
                    .show()
            }
            .addOnFailureListener {
                btn_complete.isEnabled = true
                Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "send: $it")
            }
    }
}

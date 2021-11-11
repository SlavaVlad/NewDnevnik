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
import java.util.* // ktlint-disable no-wildcard-imports
import java.util.Calendar.DAY_OF_WEEK
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

class EditHomework : Fragment(), CoroutineScope {

    val db = FirebaseFirestore.getInstance()

    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    val doc: DocumentSnapshot by lazy {
        act.toEdit!! // fixme убрать этот изврат, использовать data bus
    }

    val chips = hashMapOf<String, Chip>()

    lateinit var cg_types: ChipGroup
    lateinit var cg_subjects: ChipGroup
    lateinit var cg_targets: ChipGroup
    lateinit var btn_complete: Button
    lateinit var et_text: EditText
    lateinit var calendar: CalendarView

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

        calendar.setOnDateChangeListener { view, y, m, d ->
            data["deadline"] = Timestamp(Date(y, m+1, d))
            Toast.makeText(requireContext().applicationContext, "$y  ${m+1}  $d", Toast.LENGTH_SHORT).show()
        }
        data["deadline"] = Timestamp(Calendar.getInstance(TimeZone.getDefault()).timeInMillis/1000, 0)
        Toast.makeText(requireContext().applicationContext, data["deadline"].toString(), Toast.LENGTH_SHORT).show()


        et_text.setText(doc.getString("text"))

        loadAllChips()
        checkChipsToEdit()

        calendar.date = doc.getTimestamp("deadline")!!.toDate().toInstant().toEpochMilli()

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
            }
        }
    }

    private fun checkChipsToEdit() {
        cg_subjects.check(chips[doc.getString("subject")]!!.id)
        cg_types.check(chips[doc.getString("type")]!!.id)
    }

    private fun update() {
        data["text"] = et_text.text.toString()
        data["subject"] = requireView().findViewById<Chip>(cg_subjects.checkedChipId).text.toString()
        data["type"] = requireView().findViewById<Chip>(cg_types.checkedChipId).text.toString()
        data["completed"] = emptyMap<String, Any>()
        data["owner"] = act.user.uid!!
        data["${System.currentTimeMillis()}"] = doc // для истории версий

        if (Date(calendar.date).day != Calendar.getInstance(TimeZone.getDefault()).get(DAY_OF_WEEK)) {

            data["deadline"] = Timestamp(Date(calendar.date))

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
                    Snackbar.make(requireView(), "Задание обновлено успешно", Snackbar.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    btn_complete.isEnabled = true
                    Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "send: $it")
                }
        } else {
            act.alert("Ошибка даты", "Задание не может быть задано на сегодняшний день", "send()")
            btn_complete.isEnabled = true
        }
    }
}

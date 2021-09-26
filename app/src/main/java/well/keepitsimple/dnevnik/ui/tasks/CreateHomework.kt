package well.keepitsimple.dnevnik.ui.tasks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import java.sql.Date
import java.sql.Timestamp
import kotlin.coroutines.CoroutineContext

class CreateHomework : Fragment(), CoroutineScope {

    lateinit var date: Date
    lateinit var cg_subject: ChipGroup
    lateinit var cg_type: ChipGroup
    var chips: ArrayList<Chip> = ArrayList()
    lateinit var til_text: TextInputLayout
    lateinit var btn_complete: Button
    lateinit var calendar_i: CalendarView
    lateinit var et_text: EditText
    lateinit var pb_subj: ProgressBar
    val db = FirebaseFirestore.getInstance()
    var doc_time: com.google.firebase.Timestamp? = null
    var data = hashMapOf<String, Any>()
    var gactivity: MainActivity? = null
    var text_length: Int = 0

    private var doc_id: String? = null

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_create_homework, container, false)

        gactivity = activity as MainActivity?

        btn_complete = view.findViewById(R.id.btn_complete)

        date = Date(1970, 1, 1)

        cg_subject = view.findViewById(R.id.cg_subject)
        cg_type = view.findViewById(R.id.cg_type)
        calendar_i = view.findViewById(R.id.calendar)
        et_text = view.findViewById(R.id.et_text)
        til_text = view.findViewById(R.id.til_text)
        pb_subj = view.findViewById(R.id.pb_subj)

        launch {
            val unique_lessons: ArrayList<String> = getLessonsUniqueNames()
            pb_subj.visibility = View.GONE
            repeat(getLessonsUniqueNames().size) {

                var c = Chip(gactivity)
                c.text = unique_lessons[it]
                c.isCheckable = true
                chips.add(c)
                cg_subject.addView(c)

            }

            if (requireArguments()["edit"] == true) {
                setupUpdate(requireArguments()["doc_id"].toString())
            }

        }

            calendar_i.minDate = System.currentTimeMillis()

            cg_subject.setOnCheckedChangeListener { group, id ->
                check()
                if (id != -1) {addDataFromChip(group, requireView().findViewById(id))}
            }

            cg_type.setOnCheckedChangeListener { group, id ->
                check()
                if (id != -1) {addDataFromChip(group, requireView().findViewById(id))}
            }

            calendar_i.setOnDateChangeListener { _, year, month, dayOfMonth ->
                date = Date(year - 1900, month, dayOfMonth)
            }

            if (requireArguments().getBoolean("edit")) {
                doc_id = requireArguments().getString("doc_id")
                btn_complete.text = "Обновить уведомление"
                btn_complete.setOnClickListener {
                    completeUpdate()
                }
            } else {
                btn_complete.setOnClickListener {
                    completeAdd()
                }
            }

            et_text.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    text_length = s.length
                }
            })

            setupChips()

            return view
    }

    private suspend fun getLessonsUniqueNames(): ArrayList<String> {
        val names = ArrayList<String>()
        val docRef =  db.collection("lessons").orderBy("day", Query.Direction.ASCENDING)
        val lesson_query = docRef.get().await().query.get().await()
        repeat(lesson_query.size()) { // проходимся по каждому документу
            val lesson = lesson_query.documents[it] // получаем текущий документ
            var error = 0
            repeat(lesson.getLong("lessonsCount")!!.toInt()) { loop -> // проходимся по списку уроков в дне
                if (!names.contains(lesson.getString("${loop+1}_name")!!)) {
                    names.add(lesson.getString("${loop + 1}_name")!!)
                }
            }
        }
        return names
    }

    private fun check() {
        btn_complete.isEnabled = data.contains("subject") && data.contains("type") && text_length > 3
    }

    private fun setupUpdate(docId: String) {

        db.collection("5tasks").document(docId).get().addOnSuccessListener {

            cg_subject.setOnCheckedChangeListener { group, id ->
                check()
                if (id != -1) {addDataFromChip(group, requireView().findViewById(id))}
            }

            cg_type.setOnCheckedChangeListener { group, id ->
                check()
                if (id != -1) {addDataFromChip(group, requireView().findViewById(id))}
            }

            cg_subject.check(it.getLong("subject_id")!!.toInt())
            cg_type.check(it.getLong("type_id")!!.toInt())

            et_text.setText(it.getString("text"))

            doc_time = it.getTimestamp("deadline")!!

            calendar_i.date = doc_time!!.toDate().toInstant().toEpochMilli()

            check()

        }
    }

    private fun addDataFromChip(group: ChipGroup?, chip: Chip) {

        if (group == cg_subject) {
            data["subject"] = chip.text
            data["subject_id"] = chip.id
            calendar_i.date  = gactivity!!.getNextLesson(data["subject"] as String)*1000
            date = Date(calendar_i.date)
        }

        if (group == cg_type) {
            data["type"] = chip.text
            data["type_id"] = chip.id
        }

        check()

    }

    private fun setupChips() {
        if (requireArguments()["edit"] == true) {
            db.collection("5tasks").document(doc_id!!).get().addOnSuccessListener {
                for (c in chips) { // поиск нужного чипа
                    if (c.text == it.getString("subject")) {
                        c.isChecked = true
                    }
                    if (c.text == it.getString("type")) {
                        c.isChecked = true
                    }
                }
            }
        }
    }

    private fun completeUpdate() {

        btn_complete.isEnabled = false

        if (date != Date(1970, 1, 1)) {
            data["deadline"] = com.google.firebase.Timestamp(date)
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            db.collection("5tasks").document(doc_id!!).update(data).addOnCompleteListener {

                requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    .commitAllowingStateLoss()

                Toast.makeText(requireContext(), "Уведомление создано", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                btn_complete.isEnabled = true
            }

        } else {
            btn_complete.isEnabled = true
        }

    }

    private fun completeAdd() {
        if (date != Date(1970, 1, 1)) {
            data["deadline"] = com.google.firebase.Timestamp(date)
        } else {
            data["deadline"] = Timestamp(System.currentTimeMillis())
            gactivity!!.alert("Ошибка!", "Не удалось установить дату, будет записана текущая", "completeAdd")
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")){

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            btn_complete.isEnabled = false

            db.collection("5tasks").add(data).addOnCompleteListener {

                requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    .commitAllowingStateLoss()

                Toast.makeText(requireContext(), "Уведомление создано", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                btn_complete.isEnabled = true
            }
        }
    }
}
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
import io.grpc.Metadata
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
    lateinit var cg_target: ChipGroup
    var chips: ArrayList<Chip> = ArrayList()
    lateinit var til_text: TextInputLayout
    lateinit var btn_complete: Button
    lateinit var calendar_i: CalendarView
    lateinit var et_text: EditText
    val db = FirebaseFirestore.getInstance()
    var doc_time: com.google.firebase.Timestamp? = null
    var data = hashMapOf<String, Any>()
    var act: MainActivity? = null
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

        act = activity as MainActivity?

        btn_complete = view.findViewById(R.id.btn_complete)

        date = Date(1970, 1, 1)

        cg_subject = view.findViewById(R.id.cg_subject)
        cg_type = view.findViewById(R.id.cg_type)
        calendar_i = view.findViewById(R.id.calendar)
        et_text = view.findViewById(R.id.et_text)
        til_text = view.findViewById(R.id.til_text)
        cg_target = view.findViewById(R.id.cg_target)

        launch {
            val unique_lessons: ArrayList<String> = getChips()
            repeat(unique_lessons.size) {
                val c = Chip(act)
                c.text = unique_lessons[it]
                c.isCheckable = true
                chips.add(c)
                cg_subject.addView(c)
            }
            repeat(act !!.user.groups.size) {
                if (act !!.user.groups[it].type == "class") {
                    val c = Chip(act)
                    c.text = act !!.user.groups[it].name
                    c.isCheckable = true
                    chips.add(c)
                    cg_target.addView(c)
                }
            }
            db.collection("constants").document("hwtags").get().addOnSuccessListener {
                val types = it["types"] as List<String>
                types.forEach {
                    val c = Chip(act)
                    c.text = it
                    c.isCheckable = true
                    chips.add(c)
                    cg_type.addView(c)
                }
            }
            if (requireArguments()["edit"] == true) {
                setupUpdate(requireArguments()["doc_id"].toString())
            }

        } // Заполняем список чипов

        calendar_i.minDate = System.currentTimeMillis()

        cg_subject.setOnCheckedChangeListener { group, id ->
            if (id != View.NO_ID) {
                addDataFromChip(group, requireView().findViewById(id))
            }
            check()
        }

        cg_type.setOnCheckedChangeListener { group, id ->
            if (id != View.NO_ID) {
                addDataFromChip(group, requireView().findViewById(id))
            }
            check()
        }

        calendar_i.setOnDateChangeListener { _, year, month, dayOfMonth ->
            date = Date(year - 1900, month, dayOfMonth)
        }

        launch { isEdit() }

        et_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                text_length = s.length
                check()
            }
        })

        return view
    }

    private suspend fun isEdit() {
        if (requireArguments().getBoolean("edit")) {
            doc_id = requireArguments().getString("doc_id")
            btn_complete.text = "Обновить уведомление"
            setupChips()
            btn_complete.setOnClickListener {
                completeUpdate()
            }
        } else {
            btn_complete.setOnClickListener {
                launch{ completeAdd() }
            }
        }
    }

    private suspend fun getChips(): ArrayList<String> {

        val names = ArrayList<String>()
        val docRef = db.collection("lessons").orderBy("day", Query.Direction.ASCENDING)
        val lesson_query = docRef.get().await().query.get().await()

        repeat(lesson_query.size()) { // проходимся по каждому документу
            val lesson = lesson_query.documents[it] // получаем текущий документ
            repeat(
                lesson.getLong("lessonsCount") !!.toInt()
            ) { loop -> // проходимся по списку уроков в дне
                if (! names.contains(lesson.getString("${loop + 1}_name") !!)) {
                    names.add(lesson.getString("${loop + 1}_name") !!)
                }
            }
        }
        return names
    }

    private fun check() {
        btn_complete.isEnabled =
            data.contains("subject") && data.contains("type") && text_length > 3
    }

    private fun setupUpdate(docId: String) {

        db.collection("6tasks").document(docId).get().addOnSuccessListener { doc ->

            cg_subject.setOnCheckedChangeListener { group, id ->
                check()
                if (id != View.NO_ID) {
                    addDataFromChip(group, requireView().findViewById(id))
                }
            }

            cg_type.setOnCheckedChangeListener { group, id ->
                check()
                if (id != View.NO_ID) {
                    addDataFromChip(group, requireView().findViewById(id))
                }
            }

            chips.forEach { c ->
                when (c.text) {
                    doc.getString("subject") -> cg_subject.check(c.id)
                    doc.getString("type") -> cg_type.check(c.id)
                    //doc.getString("target") -> cg_target.check(c.id)
                }
            }

            et_text.setText(doc.getString("text"))

            doc_time = doc.getTimestamp("deadline") !!

            calendar_i.date = doc_time !!.toDate().toInstant().toEpochMilli()

            check()

        }
    }

    private fun addDataFromChip(group: ChipGroup?, chip: Chip) {

        if (group == cg_subject) {
            data["subject"] = chip.text
            data["subject_id"] = chip.id
            calendar_i.date = act !!.getNextLesson(data["subject"].toString()) * 1000
            date = Date(calendar_i.date)
        }

        if (group == cg_type) {
            data["type"] = chip.text
            data["type_id"] = chip.id
        }

        if (group == cg_target) {
            //TODO: data["groups"] =
        }

        check()

    }

    private fun setupChips() {
        db.collection("6tasks").document(doc_id !!).get().addOnSuccessListener {
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

    private fun completeUpdate() {

        btn_complete.isEnabled = false

        if (date != Date(1970, 1, 1)) {
            data["deadline"] = com.google.firebase.Timestamp(date)
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            db.collection("6tasks").document(doc_id !!).update(data).addOnCompleteListener {

                requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    .commitAllowingStateLoss()

                Toast.makeText(requireContext(), "Уведомление обновлено", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                act !!.alert("Error.F", it.message.toString(), "completeUpdate()")
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
            act !!.alert(
                "Ошибка!",
                "Не удалось установить дату, будет записана текущая",
                "completeAdd"
            )
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["school"] = act !!.user.getGroupByType("school").id !!
            data["class"] = act !!.user.getGroupByType("class").id !!
            data["text"] = et_text.text.toString()
            data["owner"] = act !!.uid !!
            data["complete"] = hashMapOf<String, Any>()

            btn_complete.isEnabled = false

            db.collection("6tasks").add(data).addOnCompleteListener {

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
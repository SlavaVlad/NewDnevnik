package well.keepitsimple.dnevnik.ui.tasks

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.R
import java.sql.Timestamp

class CreateHomework : Fragment() {

    var gyear: Int? = null
    var gmonth: Int? = null
    var gday: Int? = null
    lateinit var cg_subject: ChipGroup
    lateinit var cg_type: ChipGroup
    var btn_complete: Button? = null
    lateinit var calendar_i: CalendarView
    lateinit var et_text: EditText
    val db = FirebaseFirestore.getInstance()
    val F = "Firebase"
    var data = hashMapOf<String, Any>()

    //lateinit var user: String
    private var doc_id: String? = null

    lateinit var chips: Array<Chip>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_create_homework, container, false)

        btn_complete = view.findViewById(R.id.btn_complete)
        btn_complete

        chips = arrayOf(view.findViewById(R.id.chip4),
        view.findViewById(R.id.chip4),
        view.findViewById(R.id.chip5),
        view.findViewById(R.id.chip6),
        view.findViewById(R.id.chip7),
        view.findViewById(R.id.chip8),
        view.findViewById(R.id.chip9),
        view.findViewById(R.id.chip10),
        view.findViewById(R.id.chip11),
        view.findViewById(R.id.chip12),
        view.findViewById(R.id.chip13),
        view.findViewById(R.id.chip14),
        view.findViewById(R.id.chip15),
        view.findViewById(R.id.chip16),
        view.findViewById(R.id.chip17),
        view.findViewById(R.id.chip18),
        view.findViewById(R.id.chip19),
        view.findViewById(R.id.chip20),
        view.findViewById(R.id.chip21),
        view.findViewById(R.id.chip22)
        )


        if (requireArguments().getBoolean("edit")) {
            doc_id = requireArguments().getString("doc_id")
            setupUpdate(doc_id!!)
            btn_complete!!.setOnClickListener {
                completeUpdate()
            }
        } else {
            btn_complete!!.setOnClickListener {
                completeAdd()
            }
        }

        cg_subject = view.findViewById(R.id.cg_subject)
        cg_type = view.findViewById(R.id.cg_type)
        calendar_i = view.findViewById(R.id.calendar)
        et_text = view.findViewById(R.id.et_text)

        calendar_i.minDate = System.currentTimeMillis()

        cg_subject.setOnCheckedChangeListener { group, id ->
            if (id != -1) {
                addChipData(group, id)
            }
        }

        cg_type.setOnCheckedChangeListener { group, id ->
            if (id != -1) {
                addChipData(group, id)
            }
        }

        calendar_i.setOnDateChangeListener { view, year, month, dayOfMonth ->
            gyear = year-1900
            gmonth = month
            gday = dayOfMonth
        }

        setupChips()

        return view
    }

    private fun setupUpdate(docId: String) {

        db.collection("tasks").document(docId).get().addOnSuccessListener {

            cg_subject.setOnCheckedChangeListener { group, id ->
                if (id != -1) {
                    addChipData(group, id)
                }
            }

            cg_type.setOnCheckedChangeListener { group, id ->
                if (id != -1) {
                    addChipData(group, id)
                }
            }

            cg_subject.check(it.getLong("subject_id")!!.toInt())
            cg_type.check(it.getLong("type_id")!!.toInt())

            et_text.setText(it.getString("text"))

            val time: com.google.firebase.Timestamp = it.getTimestamp("deadline")!!

            calendar_i.date = time.seconds*1000
            gyear = time.toDate().year
            gmonth = time.toDate().month
            gday = time.toDate().day

        }

    }

    private fun addChipData(group: ChipGroup?, chip: Int) {

        var c: Chip? = null

        for(i in chips){
            if (i.id == chip){
                c = i
            }
        }

        if (group == cg_subject) {
            data["subject"] = c!!.text.toString()
            data["subject_id"] = c.id
        }

        if (group == cg_type) {
            data["type"] = c!!.text.toString()
            data["type_id"] = c.id
        }

        btn_complete!!.isEnabled = !(et_text.text.isBlank() && !data.contains("subject") && !data.contains("type"))
    }


    private fun setupChips() {

        if (requireArguments()["edit"] == true) {

            db.collection("tasks").document(doc_id!!).get().addOnSuccessListener {

                for (c in chips) {
                    if (c.text == it.getString("subject")){
                        c.isChecked = true
                    }

                    if (c.text == it.getString("type")){
                        c.isChecked = true
                    }
                }

            }
        }

            btn_complete!!.isEnabled = true

            if (requireArguments()["edit"] == true){ setupUpdate(doc_id!!) }


    }

    private fun completeUpdate() {

        btn_complete!!.isEnabled = false

        if (gyear != null) {
            data["deadline"] = Timestamp(gyear!!, gmonth!!, gday!!, 0, 0, 0, 0)
        } else {
            data["deadline"] = Timestamp(System.currentTimeMillis())
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            db.collection("tasks").document(doc_id!!).update(data).addOnCompleteListener {

                requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    .commitAllowingStateLoss()

                Toast.makeText(requireContext(), "Уведомление создано", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                btn_complete!!.isEnabled = true
            }

        } else {
            btn_complete!!.isEnabled = true
        }

    }

    private fun completeAdd() {
        if (gyear != null) {
            data["deadline"] = Timestamp(gyear!!, gmonth!!, gday!!, 0, 0, 0, 0)
        } else {
            data["deadline"] = Timestamp(System.currentTimeMillis())
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            btn_complete!!.isEnabled = false

            db.collection("tasks").add(data).addOnCompleteListener {

                requireFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.nav_host_fragment_content_main, TasksFragment())
                    .commitAllowingStateLoss()

                Toast.makeText(requireContext(), "Уведомление создано", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                btn_complete!!.isEnabled = true
            }
        }
    }
}
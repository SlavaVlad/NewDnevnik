package well.keepitsimple.dnevnik.ui.tasks

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import java.sql.Date
import java.sql.Timestamp
import kotlin.time.days

class CreateHomework : Fragment() {

    lateinit var date: Date
    lateinit var cg_subject: ChipGroup
    lateinit var cg_type: ChipGroup
    var btn_complete: Button? = null
    lateinit var calendar_i: CalendarView
    lateinit var et_text: EditText
    val db = FirebaseFirestore.getInstance()
    var doc_time: com.google.firebase.Timestamp? = null
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
            view.findViewById(R.id.chip22),
            view.findViewById(R.id.chip23)
        )

        date = Date(1970,1,1)

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
            date = Date(year-1900, month, dayOfMonth)
        }

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

        setupChips()

        return view
    }

    private fun setupUpdate(docId: String) {

        db.collection("4tasks").document(docId).get().addOnSuccessListener {

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

            doc_time = it.getTimestamp("deadline")!!

            calendar_i.date = doc_time!!.toDate().toInstant().toEpochMilli()

            //if (requireArguments()["edit"] == true) {
            //    calendar_i.setDate(time.toDate().toInstant().toEpochMilli())
            //}

        }
    }

    private fun addChipData(group: ChipGroup?, chip: Int) {

        var c: Chip? = null

        for (i in chips) {
            if (i.id == chip) {
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
    }


    private fun setupChips() {

        if (requireArguments()["edit"] == true) {
            db.collection("4tasks").document(doc_id!!).get().addOnSuccessListener {
                for (c in chips) {
                    if (c.text == it.getString("subject")) {
                        c.isChecked = true
                    }
                    if (c.text == it.getString("type")) {
                        c.isChecked = true
                    }
                }
            }
        }

        btn_complete!!.isEnabled = true

    }

    private fun completeUpdate() {

        btn_complete!!.isEnabled = false

        if (date != Date(1970,1,1)){
            data["deadline"] = com.google.firebase.Timestamp(date)
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            db.collection("4tasks").document(doc_id!!).update(data).addOnCompleteListener {

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
        if (date != Date(1970,1,1)) {
            data["deadline"] = com.google.firebase.Timestamp(date)
        } else {
            data["deadline"] = Timestamp(System.currentTimeMillis())
        }

        if (et_text.text.isNotEmpty() && data.contains("subject") && data.contains("type")) {

            data["text"] = et_text.text.toString()
            //data["owner"] = user

            btn_complete!!.isEnabled = false

            db.collection("4tasks").add(data).addOnCompleteListener {

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
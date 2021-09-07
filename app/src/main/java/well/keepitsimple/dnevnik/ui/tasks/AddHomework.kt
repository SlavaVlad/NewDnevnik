package well.keepitsimple.dnevnik.ui.tasks

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.InternalCoroutinesApi
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import java.sql.Timestamp

class AddHomework : AppCompatActivity() {

    var gyear: Int? = null
    var gmonth: Int? = null
    var gday: Int? = null
    lateinit var cg_subject: ChipGroup
    lateinit var cg_type: ChipGroup
    lateinit var btn_complete: Button
    lateinit var calendar_i: CalendarView
    lateinit var et_text: EditText
    val db = FirebaseFirestore.getInstance()
    val F = "Firebase"
    var data = hashMapOf<String, Any>()

    override fun onStart() {
        super.onStart()
        setContentView(R.layout.activity_add_homework)

        cg_subject = findViewById(R.id.cg_subject)
        cg_type = findViewById(R.id.cg_type)
        btn_complete = findViewById(R.id.btn_complete)
        calendar_i = findViewById(R.id.calendar)
        et_text = findViewById(R.id.et_text)

        calendar_i.minDate = System.currentTimeMillis()

        if (intent.getStringExtra("action") != "edit") {
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
        }

        btn_complete.setOnClickListener {
                complete()
        }

        calendar_i.setOnDateChangeListener { view, year, month, dayOfMonth ->
            gyear = year-1900
            gmonth = month
            gday = dayOfMonth
        }

        getTags()

    }

    private fun setDataFromDoc(id: String) {

        db.collection("tasks").document(id).get().addOnSuccessListener {

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

            et_text.setText(it.getString("text").toString())

            val time:com.google.firebase.Timestamp = it.getTimestamp("deadline")!!
            calendar_i.date = time.seconds*1000
            gyear = time.toDate().year
            gmonth = time.toDate().month
            gday = time.toDate().day

        }

    }

    private fun addChipData(group: ChipGroup?, id: Int) {

        val c: Chip = findViewById(id)

        if (group == cg_subject) {
            data["subject"] = c.text.toString()
            data["subject_id"] = c.id
        }
        if (group == cg_type) {
            data["type"] = c.text.toString()
            data["type_id"] = c.id
        }

        btn_complete.isEnabled =
            !(et_text.text.isBlank() && !data.contains("subject") && !data.contains("type"))
    }


    private fun getTags() {
        val docRef = db.collection("tags").document("tags")
        docRef.get()
            .addOnSuccessListener { document ->

                addChips(document)

            }
            .addOnFailureListener { exception ->
                Log.d(F, "get failed with ", exception)
            }
    }

    private fun addChips(doc_tags: DocumentSnapshot) {

        val subjects: List<String> = doc_tags.get("subjects") as List<String>
        val types: List<String> = doc_tags.get("typesofwork") as List<String>

        val check: HashMap<String, String> = hashMapOf()

        if (intent.getStringExtra("action") == "edit") {
            db.collection("tasks").document(intent.getStringExtra("id").toString()).get()
                .addOnSuccessListener {
                    check["subject"] = it.getString("subject").toString()
                    check["type"] = it.getString("type").toString()

                    for (element in subjects) {
                        val c = Chip(this)
                        c.text = element
                        c.isCheckable = true
                        cg_subject.addView(c)
                        if (element == check["subject"]){
                            c.isChecked = true
                            data["subject"] = c.text.toString()
                        }
                    }

                    for (element in types){
                        val c = Chip(this)
                        c.text = element
                        c.isCheckable = true
                        cg_type.addView(c)
                        if (element == check["type"]){
                            c.isChecked = true
                            data["type"] = c.text.toString()
                        }
                    }

                    btn_complete.isEnabled = true

                    if (intent.getStringExtra("action") == "edit") {
                        setDataFromDoc(intent.getStringExtra("id")!!)
                    }
                }

        } else {

            for (element in subjects) {
                val c = Chip(this)
                c.text = element
                c.isCheckable = true
                cg_subject.addView(c)
            }

            for (element in types){
                val c = Chip(this)
                c.text = element
                c.isCheckable = true
                cg_type.addView(c)
            }

            if (intent.getStringExtra("action") == "edit") {
                setDataFromDoc(intent.getStringExtra("id")!!)
            }
        }

    }

    private fun complete() {
            if (gyear != null) {
                data["deadline"] = Timestamp(gyear!!, gmonth!!, gday!!, 0, 0, 0, 0)
            } else {
                data["deadline"] = Timestamp(System.currentTimeMillis())
            }
            data["text"] = et_text.text.toString()

            btn_complete.isEnabled = false

            if (intent.getStringExtra("action") != "edit") {
                db.collection("tasks").add(data).addOnCompleteListener {
                    val intent1 = Intent(this, MainActivity::class.java)
                    startActivity(intent1)
                    Toast.makeText(this, "Уведомление создано", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    btn_complete.isEnabled = true
                }
            } else {
                db.collection("tasks").document(intent.getStringExtra("id")!!).update(data).addOnCompleteListener {
                    val intent1 = Intent(this, MainActivity::class.java)
                    startActivity(intent1)
                    Toast.makeText(this, "Уведомление обновлено", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    btn_complete.isEnabled = true

                }
            }
    }

}
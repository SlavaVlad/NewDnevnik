package well.keepitsimple.dnevnik.ui.lk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
class LkFragment : Fragment() {

    lateinit var rg_student:RadioGroup
    lateinit var et_school:EditText
    lateinit var et_class:EditText
    lateinit var tv_admin:TextView
    lateinit var btn_save:Button
    lateinit var rb_student:RadioButton

    lateinit var uid:String

    val F: String = "Firebase"
    val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_lk, container, false)

        rg_student = view.findViewById(R.id.rg_student)
        et_school = view.findViewById(R.id.et_school)
        et_class = view.findViewById(R.id.et_class)
        tv_admin = view.findViewById(R.id.tv_admin)
        btn_save = view.findViewById(R.id.btn_save)
        rb_student = view.findViewById(R.id.rb_student)

        val activity: MainActivity? = activity as MainActivity?
        uid = activity!!.uid!!

        db.collection("users").document(uid).get().addOnSuccessListener {
            setCabinet(it)
        }

        return view

    }

    private fun setCabinet(doc: DocumentSnapshot) {
        if (doc.getString("class")!=null){et_class.setText(doc["class"].toString())}
        if (doc.getString("school")!=null){et_school.setText(doc.getString("school"))}
        when (doc.getBoolean("isStudent")!!){
            true -> rg_student.check(R.id.rb_student)
            false -> rg_student.check(R.id.rb_teacher)
        }
        if (doc.getBoolean("isAdmin") == true){
            tv_admin.visibility = View.VISIBLE
        } else {
            tv_admin.visibility = View.GONE
        }

        btn_save.setOnClickListener {
            saveUserInfo()
        }

    }

    private fun saveUserInfo() {
        var data = hashMapOf<String, Any>()
        if (et_class.text.isNotEmpty() && et_school.text.isNotEmpty()) {
            data = hashMapOf(
                "school" to et_school.text.toString(),
                "isStudent" to rb_student.isChecked,
                "class" to et_class.text.toString()
            )
        }

        btn_save.isEnabled = false

        db.collection("users").document(uid).update(data).addOnSuccessListener {
            btn_save.isEnabled = true
            Toast.makeText(requireContext().applicationContext, "Информация записана", Toast.LENGTH_SHORT).show()
        }
    }
}
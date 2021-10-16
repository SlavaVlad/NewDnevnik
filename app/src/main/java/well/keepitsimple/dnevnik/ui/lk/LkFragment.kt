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

        return view

    }

}
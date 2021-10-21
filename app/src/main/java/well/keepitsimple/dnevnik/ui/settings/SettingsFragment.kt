package well.keepitsimple.dnevnik.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import kotlin.coroutines.CoroutineContext

class SettingsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var lay_log: LinearLayout
    lateinit var act: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        lay_log = view.findViewById(R.id.lay_log)

        val btn = view.findViewById<Button>(R.id.launch)

        btn.setOnClickListener {

            //launch()

        }

        act = activity as MainActivity

        return view

    }

    private fun log(t: String){
        val text = TextView(requireContext().applicationContext)
        text.setText(t)
        lay_log.addView(text)
    }

    /*private fun launch() {

        log("Start")

        //script

        val data = ArrayList<DocumentSnapshot>()

        db.collection("lessons")
            .get()
            .addOnSuccessListener { query ->

                query.documents.forEach { doc ->
                    data.add(doc)
                }

                var d = mapOf<String, Any>()

                d = data[0].data !!
                log("Start setting doc ${data[0].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }
                d = data[1].data!!
                log("Start setting doc ${data[1].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }
                d = data[2].data!!
                log("Start setting doc ${data[2].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }
                d = data[3].data!!
                log("Start setting doc ${data[3].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }
                d = data[4].data!!
                log("Start setting doc ${data[4].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }
                d = data[5].data!!
                log("Start setting doc ${data[5].id}")
                db.collection("lessonsgroups")
                    .document(act.user.getGroupByType("school").id.toString())
                    .collection("lessons")
                    .document()
                    .set(d)
                    .addOnSuccessListener {
                        log("Set ${d.size}")
                    }

            }



    }*/

}

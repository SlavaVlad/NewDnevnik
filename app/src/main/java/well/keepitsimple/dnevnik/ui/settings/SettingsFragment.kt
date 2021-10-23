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
import kotlinx.coroutines.tasks.await
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

    /*fun launch() {
        db.collection("users").get().addOnSuccessListener { querySnapshot ->
            log("getDocuments")
            val users = ArrayList<String>()
            querySnapshot.documents.forEach { doc ->
                users.add(doc.id)
                log("user added ${doc.id}")
            }

            var index = 0
            querySnapshot.documents.forEach{
                db.collection("users").document(it.id).update("uid", users[index])
                index++
            }

        }
    }*/

    private fun log(t: String){
        val text = TextView(requireContext().applicationContext)
        text.setText(t)
        lay_log.addView(text)
    }

}

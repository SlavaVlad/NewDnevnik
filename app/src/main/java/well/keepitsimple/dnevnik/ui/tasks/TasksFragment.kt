package well.keepitsimple.dnevnik.ui.tasks

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import well.keepitsimple.dnevnik.*
import kotlin.collections.ArrayList
import kotlin.math.ceil


class TasksFragment : Fragment() {

    val F = "Firebase"
    private lateinit var lv_tasks: ListView

    private val db = FirebaseFirestore.getInstance()

    private val tasks =
        ArrayList<TaskItem>() // динамический массив - список из полей документов в коллекции

    lateinit var fab: FloatingActionButton

    var gactivity: MainActivity? = null

    lateinit var ctx: Activity

    lateinit var pb: ProgressBar

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        ctx = activity

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        lv_tasks = view.findViewById(R.id.lv_tasks)
        fab = view.findViewById(R.id.fab_addhw)
        pb = view.findViewById(R.id.pb_loading)

        fab.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("edit", false)
            bundle.putString("user", gactivity!!.uid)
            val fragment: Fragment = CreateHomework()
            fragment.arguments = bundle
            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()
        }

        gactivity = activity as MainActivity?

        return view
    }

    override fun onStart() {
        super.onStart()

        getTasks()

    }

    private fun getTasks() {
        db.collection("4tasks").whereNotEqualTo("subject_id", null).get().addOnSuccessListener {
            for (i in 0 until it.size()) { // проходим по каждому документу
                if (getDeadlineInDays(it.documents[i].getTimestamp("deadline")) > -1 && !tasks.contains(
                        TaskItem((getDeadlineInDays(it.documents[i].getTimestamp("deadline"))),
                            it.documents[i]))
                ) {
                    tasks.add(
                        TaskItem
                            ((getDeadlineInDays(it.documents[i].getTimestamp("deadline"))), it.documents[i])
                    )
                }
            }
            setList(tasks, gactivity!!.uid!!)
        }
        //.whereEqualTo("group", "-")
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp!!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

    private fun setList(list: ArrayList<TaskItem>, uid: String) {

        val tasksAdapter = TasksAdapter(ctx.baseContext, R.layout.task_item_layout, list)

        db.collection("users").document(uid).get().addOnSuccessListener {

            lv_tasks.adapter = tasksAdapter

            //if(!it.getBoolean("isStudent")!! || it.getBoolean("isAdmin")!!) {
            lv_tasks.setOnItemClickListener { parent, view, position, id ->

                val bundle = Bundle()
                val fragment = CreateHomework()
                bundle.putBoolean("edit", true)
                bundle.putString("doc_id", tasks[position].doc.id)
                bundle.putString("user", gactivity!!.uid!!)
                fragment.arguments = bundle
                val trans: FragmentTransaction = requireFragmentManager()
                    .beginTransaction()
                    .setTransition(TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                trans.replace(R.id.nav_host_fragment_content_main, fragment)
                trans.commit()

            }
            //}

            pb.visibility = View.GONE

        }.addOnFailureListener {
            Log.w(F, "error getting documents ${it.message}")
        }

    }

    /*private fun openDoc(id: String) {
        val intent_edit:Intent = Intent(context, AddHomework::class.java) // TODO: Заменить на едит-фрагмент
            .putExtra("action", "edit")
            .putExtra("id", id)
        startActivity(intent_edit)
    }*/

}
package well.keepitsimple.dnevnik.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import well.keepitsimple.dnevnik.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil


class TasksFragment : Fragment() {

    val F = "Firebase"
    private lateinit var lv_tasks: ListView

    private val db = FirebaseFirestore.getInstance()

    private val tasks = ArrayList<TaskItem>() // динамический массив - список из полей документов в коллекции
    lateinit var MActivity: MainActivity

    var gactivity: MainActivity? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        lv_tasks = view.findViewById(R.id.lv_tasks)

        gactivity = activity as MainActivity?

        getTasks()

        return view
    }

    private fun getTasks() {
        db.collection("tasks").orderBy("deadline")
            //.whereEqualTo("group", "-")
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w(F, "Listen failed.", e) // е - ошибка
                    return@addSnapshotListener
                }

                    for (doc in value!!) { // проходим по каждому документу
                        if (getDeadlineInDays(doc.getTimestamp("deadline")) > -1 && !tasks.contains(TaskItem((getDeadlineInDays(doc.getTimestamp("deadline"))), doc))) {
                            tasks.add(
                                TaskItem
                                    ((getDeadlineInDays(doc.getTimestamp("deadline"))), doc)
                            )
                        }
                    }
                    setList(tasks, gactivity!!.uid!!)
                }

    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp!!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

    private fun setList(list: ArrayList<TaskItem>, uid: String) {

        val tasksAdapter = TasksAdapter(requireContext().applicationContext, R.layout.task_item_layout, list)

        db.collection("users").document(gactivity!!.uid!!).get().addOnSuccessListener {

            lv_tasks.adapter = tasksAdapter

            if(!it.getBoolean("isStudent")!! || it.getBoolean("isAdmin")!!) {
                lv_tasks.setOnItemClickListener { parent, view, position, id ->
                    openDoc(tasks[position].doc.id)
                }
            }

        }.addOnFailureListener {
            Log.w(F, "error getting documents ${it.message}")
        }

    }

    private fun openDoc(id: String) {
        val intent_edit:Intent = Intent(requireContext().applicationContext, AddHomework::class.java)
            .putExtra("action", "edit")
            .putExtra("id", id)
        startActivity(intent_edit)
    }

}
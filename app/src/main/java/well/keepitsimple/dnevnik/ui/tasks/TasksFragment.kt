package well.keepitsimple.dnevnik.ui.tasks

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.*
import well.keepitsimple.dnevnik.login.Group
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil


class TasksFragment : Fragment(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val F = "Firebase"
    private lateinit var lv_tasks: ListView

    private val db = FirebaseFirestore.getInstance()

    private val tasks =
        ArrayList<TaskItem>() // динамический массив - список из полей документов в коллекции

    lateinit var btnCreateHomework: FloatingActionButton

    var act: MainActivity? = null

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
        btnCreateHomework = view.findViewById(R.id.fab_addhw)
        pb = view.findViewById(R.id.pb_loading)

        btnCreateHomework.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("edit", false)
            bundle.putString("user", act!!.uid)
            val fragment: Fragment = CreateHomework()
            fragment.arguments = bundle
            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()
        }

        act = activity as MainActivity

        return view
    }

    override fun onStart() {
        super.onStart()

        launch {
            Log.d("TEST", "Launch coroutine")
            if (act!!.user.getAllPermissions() != ArrayList<String>()){
                Log.d("TEST", "SetUI")
                setUIFromPermissions(act!!.user.getAllPermissions())
            } else {
                while (act!!.user.getAllPermissions() == ArrayList<String>()) {
                    delay(50)
                    if (act!!.user.getAllPermissions() != ArrayList<String>()) {
                        Log.d("TEST", "SetUI")
                        setUIFromPermissions(act!!.user.getAllPermissions())
                    }
                }
            }
        }

    }

    private fun setUIFromPermissions(perms: ArrayList<String>) {
        val user = act!!.user
        btnCreateHomework.isVisible = perms.contains("docCreate")
        if (perms.contains("docView")) {
                getTasks(user.getGroupByType("school"), user.getGroupByType("class"))
        }
    }

    private fun getTasks(school: Group, schoolClass: Group) {
        db.collection("6tasks")
            .whereEqualTo("school", school.id)
            .whereEqualTo("class", schoolClass.id)
            .get()
            .addOnSuccessListener { querySnapshot ->

            querySnapshot.forEach { doc -> // проходим по каждому документу
                if (getDeadlineInDays(doc.getTimestamp("deadline")) > -1 && !tasks.contains(
                        TaskItem(
                            (getDeadlineInDays(doc.getTimestamp("deadline"))), doc)
                    )
                ) {
                    tasks.add(
                        TaskItem
                            ((getDeadlineInDays(doc.getTimestamp("deadline"))), doc)
                    )
                }
            }
            setList(tasks)
        }
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp!!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / DAY_S)
    }

    private fun setList(list: ArrayList<TaskItem>) {
        lv_tasks.adapter = TasksAdapter(ctx.baseContext, R.layout.task_item, list)
            if (act!!.user.checkPermission("docEdit")) {
                lv_tasks.setOnItemClickListener { parent, view, position, id ->
                    val bundle = Bundle()
                    val fragment = CreateHomework()
                    bundle.putBoolean("edit", true)
                    bundle.putString("doc_id", tasks[position].doc.id)
                    bundle.putString("user", act!!.uid)
                    fragment.arguments = bundle
                    val trans: FragmentTransaction = requireFragmentManager()
                        .beginTransaction()
                        .setTransition(TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                    trans.replace(R.id.nav_host_fragment_content_main, fragment)
                    trans.commit()
                }
            }
            pb.visibility = View.GONE
        }
}
package well.keepitsimple.dnevnik.ui.tasks

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import well.keepitsimple.dnevnik.DAY_S
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Rights
import well.keepitsimple.dnevnik.ui.groups.UserDataSetEvent
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil


const val TAG = "TasksFragment"

class TasksFragment : Fragment(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val F = "Firebase"

    lateinit var rv_tasks: RecyclerView

    private val db = FirebaseFirestore.getInstance()

    private val tasks =
        ArrayList<Task>() // динамический массив - список из полей документов в коллекции

    lateinit var btnCreateHomework: FloatingActionButton

    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    lateinit var ctx: Activity

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        ctx = activity
    }

    override fun onStart() {
        super.onStart()
        if (act.user.uid != "" && act.user.getGroupByType("class").id != null && act.user.getGroupByType("school").id != null) {
            setUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        EventBus.getDefault().register(this)

        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        rv_tasks = view.findViewById(R.id.rv_tasks)
        btnCreateHomework = view.findViewById(R.id.fabAddHomework_tasks)

        btnCreateHomework.setOnClickListener {
            val fragment: Fragment = CreateHomework()
            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()
        }
        return view
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMessageEvent(event: UserDataSetEvent?) {
        setUI()
    }

    private fun setUI() {
            btnCreateHomework.isVisible = true
                launch { getTasks() }
    }

    private fun getTasks() {
        tasks.clear()
        launch {
            var i = 0
            act.user.groupsUser.forEach {
                if (it.rights!!.contains(Rights.Doc.VIEW.string)) {
                    it.dRef!!
                        .collection("tasks")
                        .whereGreaterThan("deadline", Timestamp(Date(Instant.EPOCH.epochSecond)))
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            querySnapshot.documents.forEach { doc -> // проходим по каждому документу
                                if (!(doc.get("completed") as HashMap<String, Any>).containsKey(act.uid)
                                    &&
                                    getDeadlineInDays(doc.getTimestamp("deadline")!!) > -1
                                ) {
                                    tasks.add(
                                        Task((getDeadlineInDays(doc.getTimestamp("deadline")!!)), doc)
                                    )
                                }
                            }
                            i++
                        }.await()
                }
            }
            i = 0
            act.user.groupsAdmin.forEach {
                if (it.id != act.user.groupsUser[i].id && it.admins!![act.uid]!!.contains(Rights.Doc.VIEW.string)) {
                    it.dRef!!
                        .collection("tasks")
                        .whereGreaterThan("deadline", Timestamp(Date(Instant.EPOCH.epochSecond)))
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            querySnapshot.documents.forEach { doc -> // проходим по каждому документу
                                if (!(doc.get("completed") as HashMap<String, Any>).containsKey(act.uid)
                                    &&
                                    getDeadlineInDays(doc.getTimestamp("deadline")!!) > -1
                                ) {
                                    tasks.add(
                                        Task(
                                            (getDeadlineInDays(doc.getTimestamp("deadline")!!)),
                                            doc
                                        )
                                    )
                                }
                            }
                        }
                }
            }
        }.invokeOnCompletion {
            setList(tasks)
        }
    }

    private fun getDeadlineInDays(timestamp: Timestamp): Double {
        return ceil(((timestamp.seconds.toDouble()) - System.currentTimeMillis() / 1000) / DAY_S)
    }

    private fun setList(list: ArrayList<Task>) {
        var hwToUndoComplete: Task
        rv_tasks.layoutManager = LinearLayoutManager(requireActivity())
        val adapter = TasksRecyclerAdapter(
            list,
            object : TaskOnClickListener {
                override fun onClick(doc: DocumentSnapshot) {
                    val fragment = ViewHomework()
                    val trans: FragmentTransaction = requireFragmentManager()
                        .beginTransaction()
                        .setTransition(TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                    trans.replace(R.id.nav_host_fragment_content_main, fragment)
                    trans.commit()
                }
            },
            object : TaskOnLongClickListener {
                override fun onLongClick(doc: DocumentSnapshot, position: Int) {
                    val bottomActionDialog = BottomSheetDialog(
                        requireActivity(), R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog
                    )
                    val bottomView = LayoutInflater.from(requireActivity())
                        .inflate(
                            R.layout.modal_bottom_sheet_content,
                            requireView().findViewById(R.id.bs_container)
                        )
                    bottomView.findViewById<Button>(R.id.complete).setOnClickListener {
                        it as Button
                        Toast.makeText(requireContext(), "Сщьздуеу!!!", Toast.LENGTH_SHORT).show()
                        bottomActionDialog.dismiss()
                    }
                    bottomActionDialog.setContentView(bottomView)
                    bottomView.findViewById<Button>(R.id.complete).setOnClickListener {
                        hwToUndoComplete = list[position]
                        hwComplete(list, position, rv_tasks.adapter as TasksRecyclerAdapter)
                        Snackbar.make(
                            rv_tasks,
                            "Выполнено ${list[position].doc.getString("text")!!}",
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("Undo") {
                                hwUndoComplete(
                                    list,
                                    position,
                                    rv_tasks.adapter as TasksRecyclerAdapter,
                                    hwToUndoComplete
                                )
                            }
                            .show()
                        bottomActionDialog.dismiss()
                    }
                    bottomView.findViewById<Button>(R.id.edit).setOnClickListener {
                        act.toEdit = doc
                        val trans: FragmentTransaction = requireFragmentManager()
                            .beginTransaction()
                            .setTransition(TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                        trans.replace(R.id.nav_host_fragment_content_main, EditHomework())
                        trans.commit()
                        bottomActionDialog.dismiss()
                    }
                    bottomView.findViewById<Button>(R.id.delete).setOnClickListener {
                        val adapter = rv_tasks.adapter as TasksRecyclerAdapter
                        val builder = AlertDialog.Builder(requireActivity())
                        builder
                            .setTitle("Это действие невозможно отменить!")
                            .setMessage(
                                "Удалить задание по предмету ${
                                    list[position].doc.getString("subject")
                                }?"
                            )
                            .setCancelable(false)
                            .setPositiveButton("Удалить") { dialog, id ->
                                hwDelete(list, position, adapter)
                                Snackbar.make(
                                    rv_tasks,
                                    "Удалено ${list[position].doc.getString("text")!!}",
                                    Snackbar.LENGTH_LONG
                                )
                                    .show()
                            }
                            .setNegativeButton("Не удалять") { dialog, id ->
                                adapter.notifyItemChanged(position)
                            }
                            .setOnDismissListener {
                                adapter.notifyItemChanged(position)
                            }
                            .show()
                        bottomActionDialog.dismiss()
                    }
                    bottomActionDialog.show()
                }
            }
        )
        rv_tasks.adapter = adapter
    }

    private fun hwUndoComplete(
        list: ArrayList<Task>,
        pos: Int,
        adapter: TasksRecyclerAdapter,
        undo: Task,
    ) {

        val data = undo.doc.get("completed") as HashMap<String, Any>
        data.remove(act.uid!!)

        db.collection("groups")
            .document(act.user.getGroupByType("school").id!!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id!!)
            .collection("tasks")
            .document(undo.doc.id)
            .update("completed", data)
            .addOnSuccessListener {
                list.add(pos, undo)
                adapter.notifyItemInserted(pos)
                if (pos == 0) {
                    rv_tasks.scrollToPosition(0)
                }
            }.addOnFailureListener { exception ->
                act.alert(
                    "Ошибка запроса",
                    "Не удалось пометить как выполненное: ${exception.message}",
                    "hwComplete()"
                )
            }
    }

    private fun hwComplete(
        list: ArrayList<Task>,
        pos: Int,
        adapter: TasksRecyclerAdapter,
    ) {

        val data = list[pos].doc.get("completed") as HashMap<String, Any>
        data[act.uid!!] = true

        db.collection("groups")
            .document(act.user.getGroupByType("school").id!!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id!!)
            .collection("tasks")
            .document(list[pos].doc.id)
            .update("completed", data)
            .addOnSuccessListener {
                list.removeAt(pos)
                adapter.notifyItemRemoved(pos)
            }.addOnFailureListener { exception ->
                act.alert(
                    "Ошибка запроса",
                    "Не удалось пометить как выполненное по причине ${exception.message}",
                    "hwComplete()"
                )
            }
    }

    private fun hwDelete(
        list: java.util.ArrayList<Task>,
        pos: Int,
        adapter: TasksRecyclerAdapter
    ) {
        db.collection("groups")
            .document(act.user.getGroupByType("school").id!!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id!!)
            .collection("tasks")
            .document(list[pos].doc.id)
            .delete()
            .addOnSuccessListener {
                list.removeAt(pos)
                adapter.notifyItemRemoved(pos)
            }.addOnFailureListener { exception ->
                act.alert(
                    "Ошибка запроса",
                    "Не удалось пометить как выполненное по причине ${exception.message}",
                    "hwDelete()"
                )
            }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}

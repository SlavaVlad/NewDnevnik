package well.keepitsimple.dnevnik.ui.tasks

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.DAY_S
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Rights
import java.sql.Date
import java.time.Instant
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

    lateinit var pb: ProgressBar

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        ctx = activity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val etSortBy_tasks = view.findViewById<AutoCompleteTextView>(R.id.etSortBy_tasks)
        val etSortType_tasks = view.findViewById<AutoCompleteTextView>(R.id.etSortType_tasks)

        rv_tasks = view.findViewById(R.id.rv_tasks)
        btnCreateHomework = view.findViewById(R.id.fabAddHomework_tasks)
        pb = view.findViewById(R.id.pbLoading_tasks)

        setFilters(etSortBy_tasks, etSortType_tasks)

        btnCreateHomework.setOnClickListener {
            launch {
                if (act.isTimetablesComplete) {
                    val fragment: Fragment = CreateHomework()
                    val trans: FragmentTransaction = requireFragmentManager()
                        .beginTransaction()
                        .setTransition(TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                    trans.replace(R.id.nav_host_fragment_content_main, fragment)
                    trans.commit()
                } else {
                    while (! act.isTimetablesComplete) {
                        delay(10)
                        if (act.isTimetablesComplete) {
                            Log.d("TEST", "SetUI")
                            val fragment: Fragment = CreateHomework()
                            val trans: FragmentTransaction = requireFragmentManager()
                                .beginTransaction()
                                .setTransition(TRANSIT_FRAGMENT_OPEN)
                                .addToBackStack(null)
                            trans.replace(R.id.nav_host_fragment_content_main, fragment)
                            trans.commit()
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()

        launch {
            Log.d("TEST", "Launch coroutine")
            if (act.user.getAllPermissions() != ArrayList<String>()) {
                Log.d("TEST", "SetUI")
                setUIFromPermissions()
            } else {
                while (act.user.getAllPermissions() == ArrayList<String>()) {
                    delay(50)
                    if (act.user.getAllPermissions() != ArrayList<String>()) {
                        Log.d("TEST", "SetUI")
                        act.user = act.user
                        setUIFromPermissions()
                    }
                }
            }
        }
    }

    private fun setFilters(
        etSortBy_tasks: AutoCompleteTextView,
        etSortType_tasks: AutoCompleteTextView,
    ) {

        // фильтры 1 ===============================================================================
        val SortBy = resources.getStringArray(R.array.sortBy)
        val adapterBy = ArrayAdapter(
            requireContext().applicationContext, android.R.layout.simple_dropdown_item_1line, SortBy
        )
        etSortBy_tasks.setAdapter(adapterBy)
        // Обработчик щелчка
        etSortBy_tasks.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, id ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                val direction = when (etSortType_tasks.text.toString()) {
                    "▲" -> {
                        Query.Direction.ASCENDING
                    }
                    else -> {
                        Query.Direction.DESCENDING
                    }
                }
                // Выводим выбранное слово
                getTasksWithFilters(selectedItem, direction)
            }
        // конец фильтров 1 ========================================================================

        // фильтры 2 ===============================================================================
        val SortType = resources.getStringArray(R.array.sortType)
        val adapterType = ArrayAdapter(
            requireContext().applicationContext,
            android.R.layout.simple_dropdown_item_1line,
            SortType
        )
        etSortType_tasks.setAdapter(adapterType)
        // Обработчик щелчка
        etSortType_tasks.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, id ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                // Выводим выбранное слово
                when (selectedItem) {
                    "▲" -> {
                        getTasksWithFilters(
                            etSortBy_tasks.text.toString(),
                            Query.Direction.DESCENDING
                        )
                    }
                    "▼" -> {
                        getTasksWithFilters(
                            etSortBy_tasks.text.toString(),
                            Query.Direction.ASCENDING
                        )
                    }
                }
            }
        // конец фильтров 2 ========================================================================
    }

    private fun setUIFromPermissions() {
        btnCreateHomework.isVisible = true
        if (act.user.isAllowedInGroup(Rights.Doc.CREATE.r, act.user.getGroupByType("class"))) {
            getTasks()
        }
    }

    private fun getTasksWithFilters(sortBy: String, direction: Query.Direction) {

        if (act.isTimetablesComplete) {

            var docRef = db.collection("groups")
                .document(act.user.getGroupByType("school").id !!)
                .collection("groups")
                .document(act.user.getGroupByType("class").id !!)
                .collection("tasks")
                .whereNotEqualTo("completed", act.uid)
                .orderBy("completed")

            when (sortBy) {
                "Дедлайн" -> {
                    docRef = docRef.orderBy("deadline", direction)
                }
                "Предмет" -> {
                    docRef = docRef.orderBy("subject", direction)
                }
                "Тип" -> {
                    docRef = docRef.orderBy("type", direction)
                }
            }

            docRef.get().addOnSuccessListener { querySnapshot ->
                tasks.clear()
                Log.d("TEST", "getTasksWithFilters: success")
                querySnapshot.forEach { doc ->
                    if (getDeadlineInDays(doc.getTimestamp("deadline") !!) >= 0) {
                        tasks.add(Task((getDeadlineInDays(doc.getTimestamp("deadline") !!)), doc))
                    }
                }
                setList(tasks)
            }.addOnFailureListener {
                act.alert("Ошибка запроса", it.message.toString(), "getTasks()")
            }
        }
    }

    private fun getTasks() {
        tasks.clear()
       db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id !!)
            .collection("tasks")
            .whereGreaterThan("deadline", Timestamp(Date(Instant.EPOCH.epochSecond)))
            .orderBy("deadline", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { doc -> // проходим по каждому документу
                    doc.get("completed") as HashMap<String, Any>
                    if (!(doc
                            .get("completed") as HashMap<String, Any>)
                            .containsKey(act.uid) && getDeadlineInDays(doc.getTimestamp("deadline")!!) > -1) {
                        tasks.add(Task((getDeadlineInDays(doc.getTimestamp("deadline") !!)), doc))
                    }
                }
                setList(tasks)
            }.addOnFailureListener {
                act.alert("Ошибка запроса", "Ошибка: ${it.message}", "getTasks()")
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
                        .inflate(R.layout.modal_bottom_sheet_content,
                            requireView().findViewById(R.id.bs_container))
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
                            "Выполнено ${list[position].doc.getString("text") !!}",
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("Undo") {
                                hwUndoComplete(list,
                                    position,
                                    rv_tasks.adapter as TasksRecyclerAdapter,
                                    hwToUndoComplete)
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
                                    "Удалено ${list[position].doc.getString("text") !!}",
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

        pb.visibility = View.GONE
    }

    private fun hwUndoComplete(
        list: ArrayList<Task>,
        pos: Int,
        adapter: TasksRecyclerAdapter,
        undo: Task,
    ) {

        val data = undo.doc.get("completed") as HashMap<String, Any>
        data.remove(act.uid !!)

        db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id !!)
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
        data[act.uid !!] = true

        db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id !!)
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

    private fun hwDelete(list: java.util.ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter) {
        db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(act.user.getGroupByType("class").id !!)
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
}

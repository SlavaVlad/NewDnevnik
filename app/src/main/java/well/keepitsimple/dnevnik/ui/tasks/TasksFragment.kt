package well.keepitsimple.dnevnik.ui.tasks

import android.app.Activity
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.DAY_S
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
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

        val etSortBy_tasks = view.findViewById<AutoCompleteTextView>(R.id.etSortBy_tasks)
        val etSortType_tasks = view.findViewById<AutoCompleteTextView>(R.id.etSortType_tasks)

        rv_tasks = view.findViewById(R.id.rv_tasks)
        btnCreateHomework = view.findViewById(R.id.fabAddHomework_tasks)
        pb = view.findViewById(R.id.pbLoading_tasks)

        btnCreateHomework.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("edit", false)
            bundle.putString("user", act !!.uid)
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

        setFilters(etSortBy_tasks, etSortType_tasks)

        return view

    }

    override fun onStart() {
        super.onStart()

        launch {
            Log.d("TEST", "Launch coroutine")
            if (act !!.user.getAllPermissions() != ArrayList<String>()) {
                Log.d("TEST", "SetUI")
                setUIFromPermissions(act !!.user.getAllPermissions())
            } else {
                while (act !!.user.getAllPermissions() == ArrayList<String>()) {
                    delay(10)
                    if (act !!.user.getAllPermissions() != ArrayList<String>()) {
                        Log.d("TEST", "SetUI")
                        setUIFromPermissions(act !!.user.getAllPermissions())
                    }
                }
            }
        }

    }

    private fun setFilters(etSortBy_tasks: AutoCompleteTextView, etSortType_tasks: AutoCompleteTextView) {

        // фильтры 1 ===============================================================================
        val SortBy = resources.getStringArray(R.array.sortBy)
        val adapterBy = ArrayAdapter(
            requireContext().applicationContext, android.R.layout.simple_dropdown_item_1line, SortBy
        )
        etSortBy_tasks.setAdapter(adapterBy)
        // Обработчик щелчка
        etSortBy_tasks.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, id ->
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
            requireContext().applicationContext, android.R.layout.simple_dropdown_item_1line, SortType
        )
        etSortType_tasks.setAdapter(adapterType)
        // Обработчик щелчка
        etSortType_tasks.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            // Выводим выбранное слово
            when (selectedItem) {
                "▲" -> {
                    getTasksWithFilters(etSortBy_tasks.text.toString(), Query.Direction.DESCENDING)
                }
                "▼" -> {
                    getTasksWithFilters(etSortBy_tasks.text.toString(), Query.Direction.ASCENDING)
                }
            }
        }
        // конец фильтров 2 ========================================================================
    }

    private fun setUIFromPermissions(perms: ArrayList<String>) {
        btnCreateHomework.isVisible = perms.contains("docCreate")
        if (perms.contains("docView")) {
            getTasks()
        }
    }

    private fun getTasksWithFilters(sortBy: String, direction: Query.Direction){

        var docRef = db.collection(resources.getString(R.string.tasksVersion))
            .whereEqualTo("school", act!!.user.getGroupByType("school").id)
            .whereEqualTo("class", act!!.user.getGroupByType("class").id)
            .whereNotEqualTo("complete", act!!.uid!!)
            .orderBy("complete")

        when (sortBy){
            "Дедлайн" -> { docRef = docRef.orderBy("deadline", direction) }
            "Предмет" -> { docRef = docRef.orderBy("subject", direction) }
            "Тип" ->     { docRef = docRef.orderBy("type", direction) }
        }

        docRef.get().addOnSuccessListener { querySnapshot ->
            querySnapshot.documents
            tasks.clear()
            Log.d("TEST", "getTasksWithFilters: success")
            querySnapshot.forEach { doc -> // проходим по каждому документу
                if (getDeadlineInDays(doc.getTimestamp("deadline")!!) >= 0) {
                    tasks.add(Task((getDeadlineInDays(doc.getTimestamp("deadline")!!)), doc))
                }
            }
            setList(tasks)
        }.addOnFailureListener {
            act!!.alert("Ошибка запроса", "Ошибка: ${it.message}", "getTasks()")
        }

    }

    private fun getTasks() {
        tasks.clear()
        db.collection(resources.getString(R.string.tasksVersion))
            .whereEqualTo("school", act!!.user.getGroupByType("school").id)
            .whereEqualTo("class", act!!.user.getGroupByType("class").id)
            .whereNotEqualTo("complete", act!!.uid!!)
            .orderBy("complete")
            .orderBy("deadline", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.forEach { doc -> // проходим по каждому документу
                    if (getDeadlineInDays(doc.getTimestamp("deadline")!!) >= 0) {
                        tasks.add(Task((getDeadlineInDays(doc.getTimestamp("deadline")!!)), doc))
                    }
                }
                setList(tasks)
            }.addOnFailureListener {
                act!!.alert("Ошибка запроса", "Ошибка: ${it.message}", "getTasks()")
            }
    }

    private fun getDeadlineInDays(timestamp: Timestamp): Double {
        return ceil(((timestamp.seconds.toDouble()) - System.currentTimeMillis() / 1000) / DAY_S)
    }

    private fun setList(list: ArrayList<Task>) {

        val adapter = TasksRecyclerAdapter(list, object : TaskOnClickListener{
            override fun onClick(doc: DocumentSnapshot) {
                val bundle = Bundle()
                val fragment = ViewHomework()

                bundle.putString("subject", doc.getString("subject"))
                bundle.putString("text", doc.getString("text"))
                bundle.putString("type", doc.getString("type"))
                bundle.putString("deadline", "${getDeadlineInDays(doc.getTimestamp("deadline")!!)} дн.")

                //bundle.putString("user", act!!.user.uid)

                fragment.arguments = bundle
                val trans: FragmentTransaction = requireFragmentManager()
                    .beginTransaction()
                    .setTransition(TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)

                trans.replace(R.id.nav_host_fragment_content_main, fragment)
                trans.commit()
            }
        }, object : TaskOnLongClickListener {
            override fun onLongClick(docId: String) {
                val bundle = Bundle()
                val fragment = CreateHomework()
                bundle.putBoolean("edit", true)
                bundle.putString("doc_id", docId)
                bundle.putString("user", act!!.user.uid)

                fragment.arguments = bundle
                val trans: FragmentTransaction = requireFragmentManager()
                    .beginTransaction()
                    .setTransition(TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)

                trans.replace(R.id.nav_host_fragment_content_main, fragment)
                trans.commit()
            }
        })

        rv_tasks.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        rv_tasks.adapter = adapter

        var hwToUndoDelete: Task
        var hwToUndoComplete: Task

        val simpleCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val position = viewHolder.bindingAdapterPosition

                when (direction) {
                    
                    ItemTouchHelper.LEFT -> {
                        val builder = AlertDialog.Builder(activity!!)
                        builder
                            .setTitle("Вы уверены?")
                            .setMessage("Удалить задание по предмету ${list[position].doc.getString("subject")}?")
                            .setCancelable(false)
                            .setPositiveButton("Удалить") { dialog, id ->
                                hwToUndoDelete = list[position]
                                hwDelete(list, position, adapter)
                                Snackbar.make(rv_tasks, "Удалено задание ${list[position].doc.getString("text")!!}", Snackbar.LENGTH_LONG)
                                    .setAction("Undo") {
                                        hwUndoDelete(list, position, adapter, hwToUndoDelete)
                                    }.show()
                            }
                            .setNegativeButton("Не удалять"){ dialog, id ->
                                adapter.notifyItemChanged(position)
                            }
                            .show()
                    }
                    
                    ItemTouchHelper.RIGHT -> {
                        hwToUndoComplete = list[position]
                        hwComplete(list, position, adapter)
                        Snackbar.make(rv_tasks, "Выполнено задание ${list[position].doc.getString("text")!!}", Snackbar.LENGTH_LONG)
                            .setAction("Undo") {
                                hwUndoComplete(list, position, adapter, hwToUndoComplete)
                            }.show()
                    }
                    
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                    .addSwipeRightBackgroundColor(ContextCompat.getColor(requireContext().applicationContext, R.color.design_default_color_secondary))
                    .addSwipeRightActionIcon(R.drawable.ic_check_circle)
                    .setSwipeRightActionIconTint(R.color.design_default_color_secondary)

                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(requireContext().applicationContext, R.color.colorAccent))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete)
                    .setSwipeLeftActionIconTint(R.color.design_default_color_error)

                    .create()
                    .decorate()
            }

        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(rv_tasks)

            pb.visibility = View.GONE

        }

    private fun hwUndoDelete(list: java.util.ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter, undo: Task) {
        db.collection(resources.getString(R.string.tasksVersion))
            .document(undo.doc.id)
            .update("isDeleted", false)
            .addOnSuccessListener {
                list.add(pos, undo)
                adapter.notifyItemInserted(pos)
                if (pos == 0){ rv_tasks.scrollToPosition(0) }
            }.addOnFailureListener { exception ->
                act!!.alert("Ошибка запроса", "Не удалось отменить удаление: ${exception.message}", "hwUndoDelete()")
            }
    }

    private fun hwUndoComplete(list: ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter, undo: Task) {
        db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).get().addOnSuccessListener { refDoc ->

            val data = refDoc.get("complete") as HashMap<String, Any>
            data.remove(act!!.uid!!)

            db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).update("completed", data).addOnSuccessListener {
                list.add(pos, undo)
                adapter.notifyItemInserted(pos)
                if (pos == 0){ rv_tasks.scrollToPosition(0) }
            }.addOnFailureListener { exception ->
                act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное: ${exception.message}", "hwComplete()")
            }

        }
    }

    private fun hwComplete(list: java.util.ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter) {

        db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).get().addOnSuccessListener { refDoc ->
            if (refDoc.contains("completed")) {

                val data = refDoc.get("complete") as HashMap<String, Any>
                data[act!!.uid!!] = true

                db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).update("completed", data).addOnSuccessListener {
                    list.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                }.addOnFailureListener { exception ->
                    act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное по причине ${exception.message}", "hwComplete()")
                }

            } else {

                val data = hashMapOf<String, Any>(
                    "complete" to hashMapOf<String, Any>(
                        act!!.uid!! to true
                    )
                )

                db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).update(data).addOnSuccessListener {
                    Toast.makeText(requireContext().applicationContext, "Помечено как выполненное", Toast.LENGTH_SHORT).show()
                    list.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                }.addOnFailureListener { exception ->
                    act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное по причине ${exception.message}", "hwComplete()")
                }

            }
        }
    }

    private fun hwDelete(list: java.util.ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter) {
        db.collection(resources.getString(R.string.tasksVersion)).document(list[pos].doc.id).update("isDeleted", false).addOnSuccessListener {
            list.removeAt(pos)
            adapter.notifyItemRemoved(pos)
        }.addOnFailureListener { exception ->
            act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное по причине ${exception.message}", "hwDelete()")
        }
    }
}
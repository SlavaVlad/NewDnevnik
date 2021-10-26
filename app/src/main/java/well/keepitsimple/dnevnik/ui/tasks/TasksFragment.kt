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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.*
import well.keepitsimple.dnevnik.login.Group
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.collections.HashMap
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.size


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

    var mAdView: AdView? = null

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

        MobileAds.initialize(requireContext().applicationContext)

        mAdView = view.findViewById<AdView>(R.id.adBanner_tasks)
        val adRequest = AdRequest.Builder().build()
        mAdView!!.loadAd(adRequest)

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
                    delay(50)
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
            // Выводим выбранное слово
            Toast.makeText(requireContext().applicationContext, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext().applicationContext, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
        }
        // конец фильтров 2 ========================================================================
    }

    private fun setUIFromPermissions(perms: ArrayList<String>) {
        val user = act !!.user
        btnCreateHomework.isVisible = perms.contains("docCreate")
        if (perms.contains("docView")) {
            getTasks(user.getGroupByType("school"), user.getGroupByType("class"))
        }
    }

    private fun getTasks(school: Group, schoolClass: Group) {
        tasks.clear()
        db.collection("6tasks")
            .whereEqualTo("school", school.id)
            .whereEqualTo("class", schoolClass.id)
            .whereNotEqualTo("complete", act!!.uid!!)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.forEach { doc -> // проходим по каждому документу
                    if (getDeadlineInDays(doc.getTimestamp("deadline")) >= 0) {
                        tasks.add(Task((getDeadlineInDays(doc.getTimestamp("deadline"))), doc))
                    }
                }
                    setList(tasks)
                 // Костыль чтобы убрать дублирование списка
            }.addOnFailureListener {
                act!!.alert("Ошибка запроса", "Ошибка: ${it.message}", "getTasks()")
            }
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp !!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / DAY_S)
    }

    private fun setList(list: ArrayList<Task>) {

        if (rv_tasks.size != 0) {

            val a = rv_tasks.adapter as TasksRecyclerAdapter
            a.addList(list)

        } else {

            val adapter = TasksRecyclerAdapter(list)

            rv_tasks.layoutManager = LinearLayoutManager(requireContext().applicationContext)
            rv_tasks.adapter = adapter

            val swipeToDeleteCallback = object : SwipeToDeleteCallback() {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val pos = viewHolder.adapterPosition
                    hwComplete(list, pos, adapter)
                }
            }

            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(rv_tasks)

            if (act !!.user.checkPermission("docEdit")) {
                rv_tasks.setOnClickListener {
                    val ad = rv_tasks.adapter as TasksRecyclerAdapter
                    val bundle = Bundle()
                    val fragment = CreateHomework()
                    bundle.putBoolean("edit", true)
                    bundle.putString("doc_id", ad.getItemDocument(it) !!.id)
                    bundle.putString("user", act !!.uid)
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

    private fun hwComplete(list: java.util.ArrayList<Task>, pos: Int, adapter: TasksRecyclerAdapter) {

        db.collection("6tasks").document(list[pos].doc.id).get().addOnSuccessListener { refDoc ->
            if (refDoc.contains("completed")) {

                val data = refDoc.get("completed") as HashMap<String, Any>
                data[act!!.uid!!] = true

                db.collection("6tasks").document(list[pos].doc.id).update("completed", data).addOnSuccessListener {
                    Toast.makeText(requireContext().applicationContext, "Помечено как выполненное", Toast.LENGTH_SHORT).show()
                    list.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                }.addOnFailureListener { exception ->
                    act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное по причине ${exception.message}", "hwComplete()")
                }

            } else {

                val data = hashMapOf<String, Any>(
                    "completed" to hashMapOf<String, Any>(
                        act!!.uid!! to true
                    )
                )

                db.collection("6tasks").document(list[pos].doc.id).update(data).addOnSuccessListener {
                    Toast.makeText(requireContext().applicationContext, "Помечено как выполненное", Toast.LENGTH_SHORT).show()
                    list.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                }.addOnFailureListener { exception ->
                    act!!.alert("Ошибка запроса", "Не удалось пометить как выполненное по причине ${exception.message}", "hwComplete()")
                }

            }
        }
    }
}
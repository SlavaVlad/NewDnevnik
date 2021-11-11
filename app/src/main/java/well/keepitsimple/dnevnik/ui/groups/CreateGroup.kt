package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.createCheckableChip
import well.keepitsimple.dnevnik.getListOfStrings
import kotlin.coroutines.CoroutineContext

class CreateGroup : Fragment(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val db = FirebaseFirestore.getInstance()
    val act: MainActivity by lazy {
        activity as MainActivity
    }
    lateinit var cg_rights: ChipGroup
    lateinit var rv_users: RecyclerView
    lateinit var btn_add_group: Button
    lateinit var et_name: EditText
    lateinit var et_type: EditText
    var users: Array<MainActivity.User> = emptyArray()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_create_group, container, false)

        cg_rights = view.findViewById(R.id.cg_rights)
        rv_users = view.findViewById(R.id.rv_users)
        btn_add_group = view.findViewById(R.id.btn_add_group)
        et_name = view.findViewById(R.id.et_name)
        et_type = view.findViewById(R.id.et_type)

        launch {
            val rights =
                db.collection("constants")
                    .document("rights")
                    .get()
                    .await()
                    .getListOfStrings("rights")
            rights.forEach { right ->
                val c = createCheckableChip(requireContext(), right)
                val def = c.chipBackgroundColor
                c.isCheckedIconVisible = false
                c.chipBackgroundColor = requireContext().getColorStateList(R.color.bg_chip_state_list)
                cg_rights.addView(c)
            }
        }

        addUsers()

        btn_add_group.setOnClickListener {
            val data = HashMap<String, Any>()

            data["name"] = et_name.text.toString()
            data["type"] = et_type.text.toString()

            val a = rv_users.adapter as UsersAdapter
            data["users"] = a.checkedUsersID

            val b = ArrayList<String>()
            cg_rights.checkedChipIds.forEach {
                val c = cg_rights.findViewById<Chip>(it)
                b.add(c.text.toString())
            }
            data["rights"] = b

            db.collection("groups").document().set(data)
        }

        return view

    }

    private fun addUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { query ->
                val tmp = ArrayList<MainActivity.User>()
                query.forEach {
                    tmp.add(it.toObject<MainActivity.User>())
                }
                rv_users.layoutManager = LinearLayoutManager(requireContext().applicationContext)
                rv_users.adapter = UsersAdapter(tmp)
            }
    }
}
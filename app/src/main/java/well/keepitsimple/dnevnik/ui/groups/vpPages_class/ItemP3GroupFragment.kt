package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.omega_r.libs.omegaintentbuilder.OmegaIntentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.CreateClass
import kotlin.coroutines.CoroutineContext

class InvitesAdapter(private val info: List<String>, private val codes: List<String>, private val inviteShareClickListener: InviteShareClickListener):
    RecyclerView.Adapter<InvitesAdapter.InvitesViewHolder>() {

    class InvitesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_info)
        val code: TextView = itemView.findViewById(R.id.tv_code)
        val btnShare: ImageButton = itemView.findViewById(R.id.btn_share_invite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitesViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.invite_item, parent, false)
        return InvitesViewHolder(itemView)
    }

    override fun onBindViewHolder(h: InvitesViewHolder, position: Int) {
        with(h){
            name.text = info[position]
            code.text = codes[position]
        }
        h.btnShare.setOnClickListener {
            inviteShareClickListener.onClick(codes[position], info[position])
        }
    }

    override fun getItemCount() = info.size
}

class ItemP3GroupFragment : Fragment(), CoroutineScope {

    val act by lazy {
        requireActivity() as MainActivity
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val TAG = "ItemQrCodeGroupFragment"

    private lateinit var pf: CreateClass
    private lateinit var tv_info: TextView
    private lateinit var rv_invites: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_item_p3_finish_code_group, container, false)

        pf = requireParentFragment() as CreateClass
        tv_info = view.findViewById(R.id.tv_group_info)
        rv_invites = view.findViewById(R.id.rv_invites)

        val d = pf.data

        tv_info.text = d.toString()

        val info = requireArguments().getStringArrayList("info")
        val ids = requireArguments().getStringArrayList("ids")

        val codes = mutableListOf<String>()

        val db = FirebaseFirestore.getInstance()
        val inviteRef = db.collection("invites")

        launch {
            ids!!.forEach {
                val data = hashMapOf<String, Any>(
                    "inviteTo" to it,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                inviteRef.add(data).addOnSuccessListener { dref ->
                    codes.add(dref.id)
                }.await()
            }

        }.invokeOnCompletion {
            rv_invites.adapter = InvitesAdapter(info!!.toList(), codes.toList(),
                object :
                    InviteShareClickListener {
                    override fun onClick(id: String, info: String) {
                        OmegaIntentBuilder(context!!)
                            .share()
                            .subject("Приглашение")
                            .text("Приглашаю тебя в группу $info, для этого введи код:\n$id\n в личном кабинете приложения \"Твой дневник\"")
                            .startActivity()
                    }
                },)
            rv_invites.layoutManager = LinearLayoutManager(context)
        }

        return view
    }

}
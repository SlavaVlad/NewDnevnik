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
import com.google.firebase.dynamiclinks.ShortDynamicLink
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
import well.keepitsimple.dnevnik.ShortLinkCompletedListener
import well.keepitsimple.dnevnik.buildFirebaseLinkAsync
import well.keepitsimple.dnevnik.ui.groups.CreateClass
import kotlin.coroutines.CoroutineContext

class InvitesAdapter(
    private val info: List<String>,
    private val links: List<String>,
    private val inviteShareClickListener: InviteShareClickListener
) :
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
        with(h) {
            name.text = info[position]
            code.text = links[position]
        }
        h.btnShare.setOnClickListener {
            inviteShareClickListener.onClick(links[position], info[position])
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
        val db = FirebaseFirestore.getInstance()
        val inviteRef = db.collection("invites")

        val links = mutableListOf<String>()


        launch {

            ids!!.forEachIndexed { index, it ->

                val data = hashMapOf<String, Any>(
                    "inviteTo" to it,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                inviteRef.add(data).addOnSuccessListener { dref ->
                        val str = info!![index]
                        var compStr = ""
                        for (i in str.iterator()) {
                            if (i.toString() != ","){
                                compStr += i
                            } else {
                                break
                            }
                        }
                        buildFirebaseLinkAsync(
                            mapOf(
                                "invite" to dref.id,
                                "name" to compStr,
                            ),
                            object : ShortLinkCompletedListener {
                                override fun onCompleted(link: ShortDynamicLink) {
                                    links.add(link.shortLink.toString())
                                    if (links.size == ids.size) {
                                        rv_invites.adapter = InvitesAdapter(
                                            info.toList(),
                                            links.toList(),
                                            object :
                                                InviteShareClickListener {
                                                override fun onClick(link: String, info: String) {
                                                    OmegaIntentBuilder(context!!)
                                                        .share()
                                                        .subject("Приглашение")
                                                        .text("Приглашаю тебя в $info в приложении \"Твой дневник\": $link")
                                                        .startActivity()
                                                }
                                            },
                                        )
                                        rv_invites.layoutManager = LinearLayoutManager(context)
                                    }
                                }
                            }
                        )
                    }.await()
            }

        }

        return view
    }

}
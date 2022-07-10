package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import well.keepitsimple.dnevnik.ui.groups.vpPages_class.InviteShareClickListener
import well.keepitsimple.dnevnik.ui.groups.vpPages_class.InvitesAdapter
import kotlin.coroutines.CoroutineContext


const val TAG = "SettingsFragment"

class SettingsFragment : Fragment(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val db = FirebaseFirestore.getInstance()

    val act: MainActivity by lazy {
        activity as MainActivity
    }

    private lateinit var btnScript: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btnScript = view.findViewById(R.id.btn_script)

        btnScript.isEnabled = act.user.uid == "JEVaacnHXOWkJnbGV5ZgaRRo5n12"

        btnScript.setOnClickListener {
                script()
        }

        return view

    }

    private fun script() {

        val info = arrayListOf("Школа", "Класс")
        val ids = arrayListOf(act.user.getGroupByType("school").id, act.user.getGroupByType("class").id)
        val db = FirebaseFirestore.getInstance()
        val inviteRef = db.collection("invites")
        val links = ArrayList<String>()
        val rv_invites = requireView().findViewById<RecyclerView>(R.id.s_rv_invites)

        launch(Dispatchers.Default) {

            ids.forEachIndexed { index, it ->

                val data = hashMapOf(
                    "inviteTo" to it!!,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                inviteRef.add(data).addOnSuccessListener { dref ->
                    val str = info[index]
                    var compStr = ""
                    for (i in str.iterator()) {
                        if (i.toString() != ",") {
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

    }

}
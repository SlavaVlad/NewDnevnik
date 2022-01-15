package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.afterTextChanged
import kotlin.coroutines.CoroutineContext

class CodeEnterFragment : Fragment(), CoroutineScope {

    lateinit var et_invite: EditText
    lateinit var btn_invite: Button
    lateinit var log: TextView

    val act: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_code_enter, container, false)

        et_invite = view.findViewById(R.id.et_invite)
        btn_invite = view.findViewById(R.id.btn_invite)
        log = view.findViewById(R.id.et_log)

        et_invite.afterTextChanged {
            btn_invite.isEnabled = it.isNotEmpty()
        }

        btn_invite.setOnClickListener {
            acceptInvite(et_invite.text.toString())
        }

        return view
    }

    private fun log(text: String) {
        log.text = log.text.toString() + "[" + System.currentTimeMillis() + "]: " + text + "\n"
    }

    private fun acceptInvite(id: String) {
        val db = FirebaseFirestore.getInstance()
        log("Запрос.")
        db.collection("invites")
            .document(id)
            .get()
            .addOnSuccessListener {
                log("Запрос..")
                if (it.exists()) {
                    db.collectionGroup("groups")
                        .whereEqualTo("id", (it["inviteTo"] as String))
                        .limit(1L)
                        .get()
                        .addOnSuccessListener { q ->
                            log("Запрос...")
                            val data = hashMapOf<String, Any?>(
                                "users" to hashMapOf<String, Any?>(
                                    act.user.uid to null
                                )
                            )
                            q.documents[0].reference.set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    log("Приглашение в ${q.documents[0]["name"]} принято учпешно")
                                    launch { act.reloadGroups() }.invokeOnCompletion {
                                        log("Группы перезагружены успешно")
                                    }
                                }.addOnFailureListener {
                                    log("Ошибка при записи uid: ${it.message}")
                                }
                        }.addOnFailureListener {
                            log("Целевая группа не найдена. ${it.message}")
                        }
                } else {
                    log("Код неверен, такого приглашения в базе нет")
                }
            }.addOnFailureListener {
                log(it.message!!)
            }
    }
}
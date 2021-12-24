package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.default.SlideAdapter
import well.keepitsimple.dnevnik.login.Rights
import well.keepitsimple.dnevnik.ui.groups.vpPages_class.ItemP1GroupFragment
import well.keepitsimple.dnevnik.ui.groups.vpPages_class.ItemP3GroupFragment
import kotlin.coroutines.CoroutineContext

class CreateClass : Fragment(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    var t: ArrayList<HashMap<String, Any>> = ArrayList()

    val db = FirebaseFirestore.getInstance()

    lateinit var vpCreateGroup: ViewPager2
    val act: MainActivity by lazy {
        activity as MainActivity
    }
    var data = hashMapOf<String, Any>()

    // id документа родителя
    // название группы
    // пользователи \ код доступа
    // права, которые группа даёт пользователям
    // админы и их права
    // тип группы пишется автоматом в зависимости от родителя
    // ид расписания

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_create_group, container, false)

        vpCreateGroup = view.findViewById(R.id.vp_create_group)

        val pagerAdapter = SlideAdapter(this, mutableListOf(
            ItemP1GroupFragment(),
        ))

        vpCreateGroup.adapter = pagerAdapter

        return view

    }

    fun addDay(hm: HashMap<String, Any>){

        t.add(hm)

        if (t.size == 6) {
            val payloadGroup = hashMapOf(
                "name" to ((vpCreateGroup.adapter as SlideAdapter).getItem(0) as ItemP1GroupFragment).groupName.editText!!.text.toString(),
                "admins" to hashMapOf<String, Any>(
                    act.uid!! to listOf(
                        Rights.Doc.CREATE.r,
                        Rights.Doc.DELETE.r,
                        Rights.Doc.EDIT.r,
                        Rights.Doc.VIEW.r,
                    )
                ),
                "rights" to listOf(
                    Rights.Doc.VIEW.r,
                ),
                "type" to "class",
                "users" to hashMapOf<String, Any>()
            )
            payloadGroup["docId"] = payloadGroup.hashCode()

            db.collection("groups")
                .document(act.user.getGroupByType("school").id!!)
                .collection("groups")
                .document(payloadGroup["docId"].toString()).set(payloadGroup)

            val lRef = db.collection("groups")
                .document(act.user.getGroupByType("school").id!!)
                .collection("groups")
                .document(payloadGroup["docId"].toString())
                .collection("lessons")

            repeat(t.size) {
                lRef.document().set(t[it])
            }

            val p3 = ItemP3GroupFragment()
            val bundle = Bundle()
            bundle.putString("docId", payloadGroup["docId"].toString())
            bundle.putString("name", payloadGroup["name"].toString())
            p3.arguments = bundle
            (vpCreateGroup.adapter as SlideAdapter).insertItem(p3)
            vpCreateGroup.currentItem = 2
        }
    }

}
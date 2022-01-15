package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
        vpCreateGroup.isUserInputEnabled = false

        val pagerAdapter = SlideAdapter(
            this, mutableListOf(
                ItemP1GroupFragment(),
            )
        )

        vpCreateGroup.adapter = pagerAdapter

        return view

    }

    fun addDay(hm: HashMap<String, Any>, count: Int) {

        t.add(hm)

        if (t.size == 6) {

            val ids = ArrayList<String>()
            val info = ArrayList<String>()
            val added = ArrayList<String>()

            val user = act.user

            var mainDocRef: DocumentReference

            val mainGroup = hashMapOf(
                "name" to ((vpCreateGroup.adapter as SlideAdapter).getItem(0) as ItemP1GroupFragment).groupName.editText!!.text.toString(),
                "admins" to hashMapOf<String, Any>(
                    act.uid!! to listOf(
                        Rights.Doc.CREATE.string,
                        Rights.Doc.DELETE.string,
                        Rights.Doc.EDIT.string,
                        Rights.Doc.VIEW.string,
                    )
                ),
                "rights" to listOf(
                    Rights.Doc.VIEW.string,
                    Rights.Doc.COMPLETE.string,
                ),
                "type" to "class",
                "users" to emptyMap<String, Any?>()
            )

            user.getSchoolRef()
                .collection("groups")
                .add(mainGroup)
                .addOnSuccessListener { ref ->
                    launch {
                        mainDocRef = ref
                        val data = hashMapOf<String, Any>(
                            "id" to ref.id
                        )
                        ref.update(data)
                        ids.add(mainDocRef.id)
                        info.add(mainGroup["name"]!! as String)
                        launch {
                            t.forEach { day ->
                                var i = 0
                                (day["lessons"] as List<HashMap<String, String>>).forEach { lesson ->
                                    if (lesson.containsKey("groupId")) {
                                        if (!added.contains("${lesson["name"]!!}|${lesson["groupId"]}")) {
                                            val sbClassData = hashMapOf(
                                                "name" to lesson["name"]!!,
                                                "tag" to lesson["groupId"]!!,
                                                "admins" to hashMapOf<String, Any>(
                                                    act.uid!! to listOf(
                                                        Rights.Doc.CREATE.string,
                                                        Rights.Doc.DELETE.string,
                                                        Rights.Doc.EDIT.string,
                                                        Rights.Doc.VIEW.string,
                                                    )
                                                ),
                                                "rights" to listOf(
                                                    Rights.Doc.VIEW.string,
                                                    Rights.Doc.COMPLETE.string,
                                                ),
                                                "type" to "subclass",
                                                "users" to emptyMap<String, Any?>()
                                            )
                                            mainDocRef
                                                .collection("groups")
                                                .add(sbClassData)
                                                .addOnSuccessListener {
                                                    (day["lessons"] as List<HashMap<String, String>>)[i]["groupId"] =
                                                        it.id
                                                    val data = hashMapOf<String, Any>(
                                                        "id" to it.id
                                                    )
                                                    it.update(data)
                                                    ids.add(it.id)
                                                    added.add("${lesson["name"]!!}|${lesson["groupId"]}")
                                                    info.add("${lesson["name"]!!}, ${lesson["tag"]} группа")
                                                    i++
                                                }.await()
                                        } else {
                                            info.forEachIndexed { l, name ->
                                                if ((day["lessons"] as List<HashMap<String, String>>)[i]["name"] == name) {
                                                    (day["lessons"] as List<HashMap<String, String>>)[i]["groupId"] =
                                                        ids[l]
                                                }
                                            }
                                        }
                                    }

                                }
                                mainDocRef
                                    .collection("timetables")
                                    .document()
                                    .set(day)
                            }
                        }

                    }.invokeOnCompletion {
                        val p3 = ItemP3GroupFragment()
                        val bundle = Bundle()
                        bundle.putStringArrayList("ids", ids)
                        bundle.putStringArrayList("info", info)
                        p3.arguments = bundle
                        (vpCreateGroup.adapter as SlideAdapter).insertItem(p3)
                        vpCreateGroup.currentItem = 2
                    }
                }
        }
    }
}



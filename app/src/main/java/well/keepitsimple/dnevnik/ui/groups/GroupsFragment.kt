package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group


class GroupsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var act: MainActivity

    lateinit var fab_create_class: com.github.clans.fab.FloatingActionButton
    lateinit var fab_create_subclass: com.github.clans.fab.FloatingActionButton
    lateinit var rv_groups: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_groups, container, false)

        fab_create_class = view.findViewById(R.id.fab_create_class)
        fab_create_subclass = view.findViewById(R.id.fab_create_subclass)
        rv_groups = view.findViewById(R.id.rv_groups)
        act = activity as MainActivity

        init()
        setList()

        return view
    }

    private fun init() {
        fab_create_subclass.setOnClickListener{
            val fragment = CreateClass()
            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()
        }

        fab_create_class.setOnClickListener{

            val fragment = CreateClass()

            val args = Bundle()
            args.putString("groupType", "class")
            fragment.arguments = args

            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()

        }
    }

    private fun setList() {
        db.collectionGroup("groups")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val groups = mutableListOf<Group>()
                querySnapshot.documents.forEach { doc ->
                    groups.add(doc.toObject<Group>()!!)
                }
                rv_groups.layoutManager = LinearLayoutManager(context)
                val adapter = GroupsAdapter(
                    groups,
                    object : GroupOnClickListener {
                        override fun onClick(group: Group) {
                            val fragment = CreateClass()
                            val trans: FragmentTransaction = requireFragmentManager()
                                .beginTransaction()
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .addToBackStack(null)
                            trans.replace(R.id.nav_host_fragment_content_main, fragment)
                            trans.commit()
                        }
                    })
                rv_groups.adapter = adapter
            }
    }
}

interface GroupOnClickListener {
    fun onClick(group: Group)
}

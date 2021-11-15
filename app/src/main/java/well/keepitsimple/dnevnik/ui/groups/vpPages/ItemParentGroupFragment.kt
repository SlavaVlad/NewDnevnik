package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.groups.CreateGroup
import well.keepitsimple.dnevnik.ui.groups.GroupOnClickListener
import well.keepitsimple.dnevnik.ui.groups.GroupsAdapter

/**
 * A fragment representing a list of Items.
 */
class ItemParentGroupFragment : Fragment() {

    val act by lazy {
        requireActivity() as MainActivity
    }

    lateinit var rv_parents: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_parent_group_list, container, false)

        rv_parents = view.findViewById(R.id.vp_parents)

        getParents()

        return view
    }

    private fun getParents() {
        rv_parents.layoutManager = LinearLayoutManager(context)
        rv_parents.adapter = GroupsAdapter(act.user.groups,
            object : GroupOnClickListener {
            override fun onClick(group: Group) {
                val frag = parentFragment as CreateGroup
                frag.data["parentDocId"] = group.id!!
                frag.vpCreateGroup.setCurrentItem(1, true)
            }
        })
    }

}
package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R

class GroupsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var act: MainActivity

    lateinit var rv_groups: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_groups, container, false)

        rv_groups = view.findViewById(R.id.rv_groups)
        act = activity as MainActivity

        rv_groups.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        rv_groups.adapter = GroupsAdapter(act.user.groups)

        return view

    }
}
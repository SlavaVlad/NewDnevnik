package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.tasks.asDeferred
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R








class GroupsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var act: MainActivity

    lateinit var fab_create_group: FloatingActionButton
    lateinit var lay_groups: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_groups, container, false)


        fab_create_group = view.findViewById(R.id.fab_create_group)
        lay_groups = view.findViewById(R.id.lay_groups)
        act = activity as MainActivity

        init()

        return view

    }

    private fun init() {
        fab_create_group.setOnClickListener{
            val fragment = CreateGroup()
            val trans: FragmentTransaction = requireFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
            trans.replace(R.id.nav_host_fragment_content_main, fragment)
            trans.commit()
        }
    }
}
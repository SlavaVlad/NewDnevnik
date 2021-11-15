package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.CreateGroup

class ItemPermissionsGroupFragment : Fragment() {

    val act by lazy {
        requireActivity() as MainActivity
    }

    val TAG = "ItemPermissionsGroupFragment"

    lateinit var pf: CreateGroup

    private val sw = mutableListOf<SwitchMaterial>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_permissions_group, container, false)

        pf = requireParentFragment() as CreateGroup

        sw.add(view.findViewById(R.id.sw_doc_view))
        sw.add(view.findViewById(R.id.sw_doc_create))
        sw.add(view.findViewById(R.id.sw_doc_edit))
        sw.add(view.findViewById(R.id.sw_doc_delete))

        sw.add(view.findViewById(R.id.sw_group_create))
        sw.add(view.findViewById(R.id.sw_group_edit))
        sw.add(view.findViewById(R.id.sw_group_delete))

        if (pf.data.containsKey("rights") && pf.data["rights"] != emptyList<String>()) {
            val r = pf.data["rights"] as MutableList<String>
            val swt = sw.filter { it.tag ==  r.contains(it.tag)}
            swt.forEach {
                it.isChecked = true
            }
        }

        view.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            pf.vpCreateGroup.setCurrentItem(2, true)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val checked: List<SwitchMaterial> = sw.filter { it.isChecked }
        val listOfRights: MutableList<String> = mutableListOf()
        checked.forEach {
            listOfRights.add(it.tag as String)
        }
        repeat(checked.size) {
            pf.data["rights"] = listOfRights
        }
    }
}
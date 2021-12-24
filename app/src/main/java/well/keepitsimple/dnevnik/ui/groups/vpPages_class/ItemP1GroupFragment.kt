package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.default.SlideAdapter
import well.keepitsimple.dnevnik.login.Rights
import well.keepitsimple.dnevnik.ui.groups.CreateClass
import kotlin.coroutines.CoroutineContext

class ItemP1GroupFragment : Fragment(), CoroutineScope {

    val act by lazy {
        requireActivity() as MainActivity
    }

    lateinit var btn_ok: Button
    lateinit var et_name: TextInputEditText
    lateinit var groupName: TextInputLayout

    val db = FirebaseFirestore.getInstance()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val TAG = "ItemP1GroupFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_p1_group_list, container, false)

        btn_ok = view.findViewById(R.id.btn_ok)
        et_name = view.findViewById(R.id.et_name)
        groupName = view.findViewById(R.id.groupName)

        groupName.isErrorEnabled = true
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 0) {
                    groupName.isErrorEnabled = true
                    groupName.error = "Обязательно"
                    groupName.boxStrokeErrorColor =
                        ColorStateList.valueOf(resources.getColor(R.color.design_default_color_error))
                } else {
                    groupName.isErrorEnabled = false
                }
            }
        }

        et_name.addTextChangedListener(textWatcher)

        btn_ok.setOnClickListener { btn ->
            if (et_name.text !!.isNotEmpty()) {

                btn.isEnabled = false

                val pf = requireParentFragment() as CreateClass

                val defaultRightsClass = listOf(
                    Rights.Doc.VIEW.r,
                    Rights.Doc.COMPLETE.r,
                )
                val defaultAdmin = hashMapOf(
                    act.uid to listOf(
                        Rights.Doc.VIEW.r,
                        Rights.Doc.EDIT.r,
                        Rights.Doc.DELETE.r,
                        Rights.Doc.CREATE.r,
                        Rights.Doc.ADDFILES.r,

                        Rights.Group.CREATE.r,
                        Rights.Group.EDIT.r,
                        Rights.Group.DELETE.r,
                        Rights.Group.VIEW.r,
                    )
                )
                val docData = hashMapOf(
                    "type" to "class",
                    "rights" to defaultRightsClass,
                    "admins" to defaultAdmin,
                )

                pf.data = docData
                pf.data["name"] = et_name.text !!.toString()

                (pf.vpCreateGroup.adapter as SlideAdapter).insertItem(ItemP2GroupFragment())
                pf.vpCreateGroup.setCurrentItem(1, true)
            }
        }
        return view
    }
}
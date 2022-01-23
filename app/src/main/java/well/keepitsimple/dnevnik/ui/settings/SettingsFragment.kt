package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ShortLinkCompletedListener
import well.keepitsimple.dnevnik.buildFirebaseLinkAsync

const val TAG = "SettingsFragment"

class SettingsFragment : Fragment() {

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


        btnScript.setOnClickListener {
            script()
        }

        return view

    }

    private fun script() {
        buildFirebaseLinkAsync(mapOf("hello" to "Hello, world!"), object : ShortLinkCompletedListener {
                override fun onCompleted(link: ShortDynamicLink) {

                }
            })
        }

}
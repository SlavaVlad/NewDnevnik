package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.CodeEnterFragment


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

        view.findViewById<Button>(R.id.btn_invite).setOnClickListener {
            code()
        }

        btnScript.setOnClickListener {
            script()
        }

        return view

    }

    private fun script() {

    }

    private fun code() {
        val fragment: Fragment = CodeEnterFragment()
        val trans: FragmentTransaction = requireFragmentManager()
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(null)
        trans.replace(R.id.nav_host_fragment_content_main, fragment)
        trans.commit()
    }

}
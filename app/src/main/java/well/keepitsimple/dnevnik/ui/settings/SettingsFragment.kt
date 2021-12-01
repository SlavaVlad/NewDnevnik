package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R

class SettingsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    val act: MainActivity by lazy {
        activity as MainActivity
    }
    lateinit var btnDeadlineIsNear: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btnDeadlineIsNear = view.findViewById(R.id.btnDeadlineIsNear)

        return view

    }

}

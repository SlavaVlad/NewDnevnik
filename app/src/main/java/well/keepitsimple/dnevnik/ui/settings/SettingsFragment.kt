package well.keepitsimple.dnevnik.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.tasks.await
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import kotlin.coroutines.CoroutineContext

class SettingsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    lateinit var act: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)



        act = activity as MainActivity

        return view

    }



}

package well.keepitsimple.dnevnik.ui.lk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import kotlin.coroutines.CoroutineContext

class LkFragment : Fragment(), CoroutineScope {

    lateinit var btn_save:Button

    val gactivity by lazy {
        activity as MainActivity
    }

    lateinit var tvGroups: TextView
    lateinit var tvRights: TextView
    lateinit var etId: EditText

    val F: String = "Firebase"
    val db = FirebaseFirestore.getInstance()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_lk, container, false)

        btn_save = view.findViewById(R.id.btn_save)
        tvGroups = view.findViewById(R.id.tvGroups)
        tvRights = view.findViewById(R.id.tvRights)
        etId = view.findViewById(R.id.etId)

        //launch {
        //    getCabData()
        //}

        setUI()

        return view

    }

    private fun setUI() {

        etId.setText(gactivity.uid)

        val gNames = ArrayList<String>()
        gactivity.user.groupsUser.forEach {
            gNames.add(it.name.toString())
        }
        tvGroups.text = gNames.toString()

    }

}

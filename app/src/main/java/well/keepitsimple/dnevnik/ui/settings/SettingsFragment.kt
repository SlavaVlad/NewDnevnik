package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R


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
            qr()
        }

        btnScript.setOnClickListener {
            //script()
        }

        return view

    }

    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            acceptInvite(result.contents.toInt())
        }
    }

    private fun qr() {
        barcodeLauncher.launch(
            ScanOptions()
                .setOrientationLocked(true)
                .setPrompt("Код, который дал вам учитель")
                .setBeepEnabled(false)
        )
    }

    private fun acceptInvite(code: Int) {
        FirebaseFirestore
            .getInstance()
            .collectionGroup("groups")
            .whereEqualTo("docId", code)
            .limit(1)
            .get()
            .addOnSuccessListener { q ->
                val doc = q.documents[0]

                val update: MutableMap<String, Any> = HashMap()
                update["users"] = hashMapOf<String, Any?>(act.uid!! to null)
                doc.reference.set(update, SetOptions.merge())


                Snackbar.make(
                    requireView(),
                    "Вы вступили в группу ${doc["name"]}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

}
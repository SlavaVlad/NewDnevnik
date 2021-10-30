package well.keepitsimple.dnevnik.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class NotificationActionReceiver: BroadcastReceiver() {

    val db = FirebaseFirestore.getInstance()

    override fun onReceive(p0: Context?, p1: Intent) {

        val uid = p1.getStringExtra("uid")
        val docUid = p1.getStringExtra("docUid")

        db.collection("6tasks")
            .document(docUid!!)
            .get()
            .addOnSuccessListener { refDoc ->

            if (refDoc.contains("completed")) {

                val data = refDoc.get("completed") as HashMap<String, Any>
                data[uid!!] = true

                db.collection("6tasks")
                    .document(docUid)
                    .update("completed", data)
                    .addOnSuccessListener {
                    Log.d("DEBUG", "onReceive: completed from notification $docUid")
                }.addOnFailureListener { exception ->
                    Log.e("DEBUG", "onReceive: failed completing from notification $docUid")
                }

            } else {

                val data = hashMapOf<String, Any>(
                    "completed" to hashMapOf<String, Any>(
                        uid!! to true
                    )
                )

                db.collection("6tasks")
                    .document(docUid)
                    .update(data)
                    .addOnSuccessListener {
                    Log.d("DEBUG", "onReceive: completed from notification $docUid")
                }.addOnFailureListener { exception ->
                    Log.e("DEBUG", "onReceive: failed completing from notification $docUid")
                }

            }
        }.addOnFailureListener {
            Log.e("DEBUG", "onReceive: get collection failed")
        }
    }

}
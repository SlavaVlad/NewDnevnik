package well.keepitsimple.dnevnik.ui.tasks

import com.google.firebase.firestore.DocumentSnapshot

interface TaskOnClickListener {
    fun onClick(doc: DocumentSnapshot) // view
}
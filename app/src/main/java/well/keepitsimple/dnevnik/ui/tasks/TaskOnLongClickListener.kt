package well.keepitsimple.dnevnik.ui.tasks

import com.google.firebase.firestore.DocumentSnapshot

interface TaskOnLongClickListener {
    fun onLongClick(doc: DocumentSnapshot) // edit
}
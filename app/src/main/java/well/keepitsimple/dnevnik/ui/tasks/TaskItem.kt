package well.keepitsimple.dnevnik.ui.tasks

import com.google.firebase.firestore.DocumentSnapshot

data class TaskItem(
    val deadline: Double,
    val doc: DocumentSnapshot
        )

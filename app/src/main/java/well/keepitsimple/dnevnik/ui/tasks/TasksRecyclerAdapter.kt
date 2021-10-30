package well.keepitsimple.dnevnik.ui.tasks

import android.content.ContentProvider
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.graphics.toColor
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import well.keepitsimple.dnevnik.R
import androidx.recyclerview.widget.ItemTouchHelper.Callback.makeMovementFlags

import androidx.recyclerview.widget.ItemTouchHelper

class TasksRecyclerAdapter(private val tasks: ArrayList<Task>) :
    RecyclerView.Adapter<TasksRecyclerAdapter.TaskHolder>() {

    val docs = mutableMapOf<View, DocumentSnapshot>()

    inner class TaskHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var subject = itemView.findViewById<TextView>(R.id.subject)
        var deadline = itemView.findViewById<TextView>(R.id.deadline)
        var text = itemView.findViewById<TextView>(R.id.text)
        var type = itemView.findViewById<TextView>(R.id.type)
    }

    fun onItemDismiss(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskHolder(itemView)
    }

    override fun onBindViewHolder(h: TaskHolder, position: Int) {

        val t = tasks[position]
        h.subject.text = t.doc.getString("subject")
        h.text.text = t.doc.getString("text")
        h.type.text = t.doc.getString("type")
        when (t.deadline) {
            1.0 -> { // deadline = 1 -> завтра
                h.deadline.text = "Завтра"
                //h.deadline.setTextColor(context.getColor(R.color.colorAccent))
            }
            0.0 -> { // deadline = 0 -> сегодня
                h.deadline.text = "Сегодня"
                //h.deadline.setTextColor(context.getColor(R.color.design_default_color_error))
            }
            else -> { // deadline > 1 -> Сдача через Н дней
                h.deadline.text = ((t.deadline).toInt().toString() + " дн.")
                //h.deadline.setTextColor(context.getColor(R.color.design_default_color_secondary))
            }
        }

        docs[h.itemView] = t.doc

    }

    override fun getItemCount() = tasks.size

    fun getItemDocument(v: View): DocumentSnapshot? {
        return docs[v]
    }
}

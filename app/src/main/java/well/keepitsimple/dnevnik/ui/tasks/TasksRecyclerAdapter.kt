package well.keepitsimple.dnevnik.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.R

class TasksRecyclerAdapter(private val tasks: ArrayList<Task>, private val onClickListener: TaskOnClickListener, private val onLongClickListener: TaskOnLongClickListener) :
    RecyclerView.Adapter<TasksRecyclerAdapter.TaskHolder>() {

    class TaskHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subject = itemView.findViewById<TextView>(R.id.subject)
        val deadline = itemView.findViewById<TextView>(R.id.deadline)
        val text = itemView.findViewById<TextView>(R.id.text)
        val type = itemView.findViewById<TextView>(R.id.type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
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
                //h.deadline.setTextColor(getColor(R.color.design_default_color_secondary))
            }
        }

        h.itemView.setOnClickListener {
            onClickListener.onClick(tasks[position].doc)
        }

        h.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(tasks[position].doc, position, )
            true
        }

    }

    override fun getItemCount() = tasks.size

}

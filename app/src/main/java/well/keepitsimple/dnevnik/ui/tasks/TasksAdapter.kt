package well.keepitsimple.dnevnik.ui.tasks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import well.keepitsimple.dnevnik.R

class TasksAdapter(var ctx: Context, var ressource: Int, var item: ArrayList<Task>) :
    ArrayAdapter<Task>(ctx, ressource, item) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(ctx)
        val view = layoutInflater.inflate(ressource, null)

        val item = item[position].doc

        val subject = view.findViewById<TextView>(R.id.subject)
        val deadline = view.findViewById<TextView>(R.id.deadline)
        val text = view.findViewById<TextView>(R.id.text)
        val type = view.findViewById<TextView>(R.id.type)

        subject.text = item.getString("subject")
        type.text = item.getString("type")
        text.text = item.getString("text")

        when (this.item[position].deadline) {
            1.0 -> {// deadline = 1 -> сдача завтра
                deadline.text = "Завтра"
                deadline.setTextColor(context.getColor(R.color.colorAccent))
            }
            0.0 -> {// deadline = 0 -> сегодня
                deadline.text = "Сегодня"
                deadline.setTextColor(context.getColor(R.color.design_default_color_error))
            }
            else -> {// deadline > 1 -> Сдача через Н дней
                deadline.text = ((this.item[position].deadline).toInt().toString() + " дн.")
                deadline.setTextColor(context.getColor(R.color.design_default_color_secondary))
            }
        }

        return view
    }
}
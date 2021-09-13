package well.keepitsimple.dnevnik.ui.tasks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import well.keepitsimple.dnevnik.R

class TasksAdapter(var ctx: Context, var ressource: Int, var item: ArrayList<TaskItem>) :
    ArrayAdapter<TaskItem>(ctx, ressource, item) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(ctx)
        val view = layoutInflater.inflate(ressource, null)

        val item = item[position].doc

        val subject = view.findViewById<TextView>(R.id.i_subject)
        val deadline = view.findViewById<TextView>(R.id.i_deadline)
        val text = view.findViewById<TextView>(R.id.i_text)
        val type = view.findViewById<TextView>(R.id.i_type)

        subject.text = item.getString("subject")
        subject.setTextColor(context.getColor(R.color.design_default_color_primary_variant))

        type.text = item.getString("type")
        type.setTextColor(context.getColor(R.color.design_default_color_primary))

        text.text = item.getString("text")
        text.setTextColor(context.getColor(R.color.design_default_color_primary_dark))

        when (this.item[position].deadline) {
            1.0 -> {// deadline = 0 -> сдача завтра
                deadline.text = "Сдача завтра"
                deadline.setTextColor(context.getColor(R.color.colorAccent))
            }
            //0.0 -> {// deadline = 1 -> сегодня
            //    deadline.text = "Сдача сегодня"
            //    deadline.setTextColor(context.getColor(R.color.colorAccent))
            //}
            else -> {// deadline > 1 -> Сдача через Н дней
                deadline.text = ((this.item[position].deadline).toInt().toString() + " дн.")
                deadline.setTextColor(context.getColor(R.color.design_default_color_secondary))
            }
        }

        return view
    }
}
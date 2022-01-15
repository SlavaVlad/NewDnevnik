package well.keepitsimple.dnevnik.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import well.keepitsimple.dnevnik.R

class ViewHomework : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_homework, container, false)

        val subject = view.findViewById<TextView>(R.id.v_subject)
        val type = view.findViewById<TextView>(R.id.v_type)
        val text = view.findViewById<TextView>(R.id.v_text)
        val deadline = view.findViewById<TextView>(R.id.v_deadline)

        with(requireArguments()) {
            subject.text = "Предмет ${getString("subject")}"
            type.text = "Тип ${getString("type")}"
            text.text = getString("text")
            deadline.text = "Сдача через ${getString("deadline")} дн."
            when (getInt("deadline")) {
                1 -> { // deadline = 1 -> завтра
                    deadline.text = "Завтра"
                    //h.deadline.setTextColor(context.getColor(R.color.colorAccent))
                }
                0 -> { // deadline = 0 -> сегодня
                    deadline.text = "Сегодня"
                    //h.deadline.setTextColor(context.getColor(R.color.design_default_color_error))
                }
                else -> { // deadline > 1 -> Сдача через Н дней
                    deadline.text = "Сдача через ${getString("deadline")} дн."
                    //h.deadline.setTextColor(getColor(R.color.design_default_color_secondary))
                }
            }
        }

        return view
    }

}
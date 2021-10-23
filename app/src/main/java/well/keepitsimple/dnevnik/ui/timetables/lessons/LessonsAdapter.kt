package well.keepitsimple.dnevnik.ui.timetables.lessons

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.timetables.Lesson


class LessonsAdapter(var ctx: Context, var ressource: Int, var d_item: ArrayList<Lesson>) :
    ArrayAdapter<Lesson>(ctx, ressource, d_item) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(ctx)
        val view = layoutInflater.inflate(ressource, null)

        val item = d_item[position]

        val name = view.findViewById<TextView>(R.id.subj)
        val time = view.findViewById<TextView>(R.id.time)
        val cab = view.findViewById<TextView>(R.id.cab)

        name.text = item.name
        cab.text = item.cab.toString()

        time.text = "${item.startAt} - ${item.endAt}"

        return view
    }



}
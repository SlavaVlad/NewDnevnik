package well.keepitsimple.dnevnik.ui.timetables.lessons

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.timetables.TimetablesFragment
import androidx.viewpager.widget.PagerAdapter.POSITION_NONE




class LessonsAdapter(var ctx: Context, var ressource: Int, var d_item: ArrayList<TimetablesFragment.Lesson>) :
    ArrayAdapter<TimetablesFragment.Lesson>(ctx, ressource, d_item) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(ctx)
        val view = layoutInflater.inflate(ressource, null)

        val item = d_item[position]

        val name = view.findViewById<TextView>(R.id.name)
        val time = view.findViewById<TextView>(R.id.time)
        val cab = view.findViewById<TextView>(R.id.cab)
        val type = view.findViewById<TextView>(R.id.type)

        name.text = item.name
        cab.text = item.cab.toString()
        type.text = item.type

        time.text = "${item.startAt} - ${item.endAt}"

        return view
    }



}
package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.afterTextChanged

class FragmentCreateTimetablePage : Fragment() {

    val act by lazy {
        requireActivity() as MainActivity
    }

    lateinit var rv_timetable: RecyclerView
    lateinit var et_offset: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_timetable_page, container, false)
        rv_timetable = view.findViewById(R.id.rv_timetable)
        et_offset = view.findViewById(R.id.et_offset)

        rv_timetable.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        rv_timetable.adapter = TimetableCreateAdapter(ArrayList())

        return view
    }

}

data class DataModelLesson(
    var name: String,
    var cab: Int,
)

class LessonsData {

    private val lessons = ArrayList<DataModelLesson>()

    fun saveLesson(name: String, cab: String) {
        lessons.add(DataModelLesson(name, cab.toInt()))
    }

    fun saveCab(position: Int, cab: String) {
        lessons[position].cab = cab.toInt()
    }
    fun saveName(position: Int, name: String) {
        lessons[position].name = name
    }

    fun getLesson(pos: Int): DataModelLesson {
        return lessons[pos]
    }

    fun remove(pos: Int) {
        lessons.removeAt(pos)
    }

}

class TimetableCreateAdapter(var items: ArrayList<Int>) :
    RecyclerView.Adapter<TimetableCreateAdapter.TimetableCreateCardHolder>() {

    val itemsHm: ArrayList<HashMap<String, TextInputEditText>> = ArrayList()

    val data = LessonsData()

    class TimetableCreateCardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextInputEditText = itemView.findViewById(R.id.et_lesson_name)
        var cab: TextInputEditText = itemView.findViewById(R.id.et_lesson_cab)
        var delete: AppCompatImageView = itemView.findViewById(R.id.imBtn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableCreateCardHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_timetable, parent, false)
        return TimetableCreateCardHolder(itemView)
    }

    override fun onBindViewHolder(h: TimetableCreateCardHolder, position: Int) {

        data.saveLesson("", "-1")

        val hm = hashMapOf(
            "name" to h.name,
            "cab" to h.cab,
        )

        h.name.setText(data.getLesson(position).name)

        if (data.getLesson(position).cab != -1) {
            h.cab.setText(data.getLesson(position).cab.toString())
        }

        h.name.afterTextChanged {
            data.saveName(position, it)
        }
        h.cab.afterTextChanged {
            data.saveCab(position, it)
        }

        itemsHm.add(hm)
        h.delete.setOnClickListener {
            data.remove(position)
            itemsHm.removeAt(position)
            notifyItemRemoved(position)
        }

        if (h.name.text !!.isEmpty() || h.cab.text !!.isEmpty()) {
            h.name.requestFocus()
        }
    }

    override fun getItemCount() = items.size

    fun insertItem() {
        items.add(0)
        notifyItemInserted(items.size)
    }

}
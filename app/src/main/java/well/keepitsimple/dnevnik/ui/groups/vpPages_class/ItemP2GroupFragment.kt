package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.omega_r.libs.omegaintentbuilder.OmegaIntentBuilder
import com.omega_r.libs.omegaintentbuilder.handlers.ActivityResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.apache.poi.ss.usermodel.WorkbookFactory
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.getNumToString
import well.keepitsimple.dnevnik.ui.groups.CreateClass
import well.keepitsimple.dnevnik.ui.timetables.objects.Lesson
import well.keepitsimple.dnevnik.ui.timetables.objects.Timetable
import kotlin.coroutines.CoroutineContext

class ItemP2GroupFragment : Fragment(), CoroutineScope {

    val db = FirebaseFirestore.getInstance()

    val TAG = "ItemP2GroupFragment"

    val act: MainActivity by lazy {
        activity as MainActivity
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    lateinit var btn_load: Button
    lateinit var next: Button
    lateinit var getRef: Button

    var timetable: Timetable? = null

    // id документа родителя
    // название группы
    // пользователи \ код доступа
    // права, которые группа даёт пользователям
    // админы и их права
    // тип группы пишется автоматом в зависимости от родителя
    // ид расписания

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_item_p2_create_timetable_group, container, false)

        Log.d(TAG, "onCreateView: CREATE_VIEW")

        btn_load = view.findViewById(R.id.btn_load)
        next = view.findViewById(R.id.next)
        getRef = view.findViewById(R.id.btn_get_ref)

        btn_load.setOnClickListener {
            OmegaIntentBuilder
                .from(requireActivity())
                .pick()
                .file()
                .createIntentHandler(requireActivity())
                .startActivityForResult(object : ActivityResultCallback {
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        if (data?.data != null && parseTimetable(data.data!!) != null) {
                            timetable = parseTimetable(data.data!!)
                            next.isEnabled = true
                        } else {
                            act.alert(
                                "Не удалось преобразовать расписание",
                                "Проверьте, что формат файла .xls и таблица составлена строго по шаблону",
                                "parseTimetable()"
                            )
                            next.isEnabled = false
                        }
                    }
                })
        }

        next.setOnClickListener {
            finish()
        }

        getRef.setOnClickListener {

        }

        return view
    }

    private fun parseTimetable(uri: Uri): Timetable? {
        try {
            val wb = WorkbookFactory.create(act.contentResolver.openInputStream(uri))
            val lessons = ArrayList<Lesson>()
            wb.sheetIterator().withIndex().forEachRemaining { _sheet ->
                val dow = _sheet.index
                val sheet = _sheet.value
                sheet.removeRow(sheet.getRow(0)) // удалили шапку
                sheet.rowIterator().forEachRemaining { row -> // проходимся по урокам
                    while (row.getCell(0) != null) {
                        with(row) {
                            val ls = Lesson(
                                getCell(0).numericCellValue.toInt() - 1,
                                getCell(2).getNumToString(),
                                getCell(1).stringCellValue,
                                getCell(3).stringCellValue,
                                act.docTime[getCell(0).numericCellValue.toInt() - 1],
                                dow + 1,
                            )
                            if (getCell(4) != null) {
                                ls.groupId = getCell(4).getNumToString()
                                ls.tag = getCell(4).getNumToString()
                            }
                            lessons.add(ls)
                        }
                        break
                    }
                }
            }
            return Timetable(lessons)
        } catch (e: Exception) {
            Log.e(TAG, "parseTimetable: $e")
            return null
        }
    }

//(item.lessons[it][0] as TextInputEditText).text.toString() - имя предмета txt
//(item.lessons[it][1] as TextInputEditText).text.toString() - кабинет int
//(item.lessons[it][2] as TextInputEditText).text.toString() - индекс группы, если не "0" int

    private fun finish() {
        val pf = requireParentFragment() as CreateClass
        pf.uploadClass(timetable!!)
    }
}
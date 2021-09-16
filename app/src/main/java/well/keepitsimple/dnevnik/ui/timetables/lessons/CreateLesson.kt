package well.keepitsimple.dnevnik.ui.timetables.lessons

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import well.keepitsimple.dnevnik.R

class CreateLesson : Fragment() {

    val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_lesson, container, false)

        //TODO: Дописать добавление урока (Название, номер, время, тип)

        return view
    }

}
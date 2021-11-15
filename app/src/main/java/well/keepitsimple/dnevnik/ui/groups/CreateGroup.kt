package well.keepitsimple.dnevnik.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.vpPages.ItemParentGroupFragment
import well.keepitsimple.dnevnik.ui.groups.vpPages.ItemPermissionsGroupFragment
import kotlin.coroutines.CoroutineContext

class CreateGroup : Fragment(), CoroutineScope {

    private var job: Job = Job()

    lateinit var vpCreateGroup: ViewPager2

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    val db = FirebaseFirestore.getInstance()
    val act: MainActivity by lazy {
        activity as MainActivity
    }
    val data = hashMapOf<String, Any>()
    // id документа родителя
    // название группы
    // пользователи \ код доступа
    // права, которые группа даёт пользователям
    // админы и их права
    // тип группы пишется автоматом в зависимости от родителя
    // ид расписания
    //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_create_group, container, false)

        vpCreateGroup = view.findViewById(R.id.vp_create_group)

        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> {
                        ItemParentGroupFragment()
                    }
                    else -> {
                        ItemPermissionsGroupFragment()
                    }
                }
            }

            override fun getItemCount() = 3
        }

        vpCreateGroup.adapter = pagerAdapter

        return view
    }

}
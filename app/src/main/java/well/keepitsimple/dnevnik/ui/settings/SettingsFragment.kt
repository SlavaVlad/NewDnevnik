package well.keepitsimple.dnevnik.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import well.keepitsimple.dnevnik.notifications.NotificationsMainService


class SettingsFragment : Fragment() {

    val db = FirebaseFirestore.getInstance()
    val act: MainActivity by lazy {
        activity as MainActivity
    }
    lateinit var btnDeadlineIsNear: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btnDeadlineIsNear = view.findViewById(R.id.btnDeadlineIsNear)
        btnDeadlineIsNear.setOnClickListener {
            askTime()
        }

        return view

    }

    private fun askTime() {
        val isSystem24Hour = is24HourFormat(requireContext().applicationContext)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setTitleText("Select Appointment time")
            .build()
        picker.show(parentFragmentManager, "not_hw")
        picker.addOnPositiveButtonClickListener {
            saveInt("btnDeadlineIsNear_minute", picker.minute)
            val intent_m = Intent(context, NotificationsMainService::class.java)
                .putExtra("settingsUpdate", "btnDeadlineIsNear_minute")
            requireActivity().startService(intent_m)
            saveInt("btnDeadlineIsNear_hour", picker.hour)
            val intent_h = Intent(context, NotificationsMainService::class.java)
                .putExtra("settingsUpdate", "btnDeadlineIsNear_hour")
            requireActivity().startService(intent_h)
        }
    }

    fun saveInt(key: String, value: Int) {
        val sPref = this.requireActivity().getSharedPreferences("uid", MODE_PRIVATE)
        val ed: SharedPreferences.Editor = sPref.edit()
        ed.putInt(key, value)
        ed.apply()
        Log.d("sPref", "saveInt: $key ; $value")
    }

}

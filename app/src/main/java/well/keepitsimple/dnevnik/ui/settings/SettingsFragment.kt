package well.keepitsimple.dnevnik.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import well.keepitsimple.dnevnik.R

class SettingsFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {

    return inflater.inflate(R.layout.fragment_settings, container, false)
  }

}


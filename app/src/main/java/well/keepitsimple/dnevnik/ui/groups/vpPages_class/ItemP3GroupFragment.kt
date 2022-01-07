package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.omega_r.libs.omegaintentbuilder.OmegaIntentBuilder
import net.glxn.qrgen.android.QRCode
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.CreateClass


class ItemP3GroupFragment : Fragment() {

    private var bmp: Bitmap? = null
    val act by lazy {
        requireActivity() as MainActivity
    }

    val TAG = "ItemQrCodeGroupFragment"

    private lateinit var qrImgView: ImageView
    private lateinit var pf: CreateClass
    private lateinit var btn_create_group: ImageView
    private lateinit var tv_info: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_item_p3_finish_code_group, container, false)

        qrImgView = view.findViewById(R.id.qrCode)
        pf = requireParentFragment() as CreateClass
        tv_info = view.findViewById(R.id.tv_group_info)
        btn_create_group = view.findViewById(R.id.btn_create_group)

        val d = pf.data

        tv_info.text = d.toString()

        //createInvite(requireArguments().getString("docId", ""))

        btn_create_group.setOnClickListener {
            OmegaIntentBuilder.from(requireContext())
                .share()
                .bitmap(bmp!!)
                .createIntentHandler()
                .chooserTitle("Приглашаю в группу ${requireArguments().getString("name", "")}")
                .startActivity()
        }

        return view
    }

    private fun createInvite(code: String) {
        bmp = QRCode.from(code).bitmap()
        qrImgView.setImageBitmap(bmp)
    }
}
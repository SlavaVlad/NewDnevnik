package well.keepitsimple.dnevnik.ui.groups.vpPages

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import net.glxn.qrgen.android.QRCode
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.ui.groups.CreateGroup
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream


class ItemP3GroupFragment : Fragment() {

    val act by lazy {
        requireActivity() as MainActivity
    }

    val TAG = "ItemQrCodeGroupFragment"

    private lateinit var qrImgView: ImageView
    private lateinit var pf: CreateGroup
    private lateinit var btn_create_group: Button
    private lateinit var tv_info: TextView

    private val invite = hashMapOf<String, Any>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_p3_finish_code_group, container, false)

        qrImgView = view.findViewById(R.id.qrCode)
        pf = requireParentFragment() as CreateGroup
        tv_info = view.findViewById(R.id.tv_info)
        btn_create_group = view.findViewById(R.id.btn_create_group)

        invite["owner"] = act.user.uid !!
        invite["timestamp"] = Timestamp.now()
        invite["groups_invited"] = listOf(
            pf.data["name"]
        )

        val d = pf.data

        tv_info.text = d.toString()

        btn_create_group.setOnClickListener {
            setData()
        }

        return view
    }

    private fun setData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("groups")
            .document(act.user.getGroupByType("school").id !!)
            .collection("groups")
            .document(pf.data.hashCode().toString())
            .set(pf.data)
            .addOnSuccessListener {
                val batch = db.batch()
                pf.t.forEach {
                    val docRef = db.collection("groups")
                        .document(act.user.getGroupByType("school").id !!)
                        .collection("groups")
                        .document(pf.data.hashCode().toString())
                        .collection("lessons")
                        .document()
                    batch.set(docRef, it)
                }
                batch.commit().addOnSuccessListener {
                    db.collection("groups")
                        .document(act.user.getGroupByType("school").id !!)
                        .collection("groups")
                        .document(pf.data.hashCode().toString())
                        .collection("tasks")
                        .document()
                        .set(hashMapOf(
                            "completed" to hashMapOf<String, Any>(),
                            "deadline" to Timestamp(System.currentTimeMillis() / 1000 + 86400, 0),
                            "owner" to act.uid,
                            "subject" to pf.t[0]["0_name"],
                            "text" to "Если вы видите это задание в списке, то это значит, что группа ${pf.data["name"]} создана успешно. Вы можете удалить данную запись из списка через длинное нажатие на задание.",
                            "type" to "Д/з"
                        )).addOnSuccessListener {
                            db.collection("invites")
                                .document(invite.hashCode().toString())
                                .set(invite)
                                .addOnSuccessListener {
                                    qrImgView.setImageBitmap(QRCode.from(invite.hashCode()
                                        .toString()).bitmap())
                                    saveImage(QRCode.from(invite.hashCode().toString()).bitmap(),
                                        requireContext(),
                                        "invites")
                                }.addOnFailureListener {
                                    act.alert("Ошибка запроса",
                                        it.message.toString(),
                                        "onStart, ItemQrCodeGroupFragment")
                                }
                        }
                }
            }
    }

    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory =
                File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
            // getExternalStorageDirectory is deprecated in API 29

            if (! directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            val values = contentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun contentValues(): ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import well.keepitsimple.dnevnik.R

class FragmentCreateTimetablePage : Fragment() {

    val lessonsCount = 10

    lateinit var lay_0: LinearLayout
    lateinit var etOffset: EditText

    val lessons = ArrayList<ArrayList<TextInputEditText>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_timetable_page, container, false)

        lay_0 = view.findViewById(R.id.lay_lessons)
        etOffset = view.findViewById(R.id.et_offset)

        repeat(lessonsCount) {
            createEditText(it)
        }


        return view
    }

    private fun createEditText(i: Int) {

        val ctx: Context = ContextThemeWrapper(requireActivity().baseContext, R.style.Theme_MaterialComponents_Light)

        val cv_1 = CardView(ctx)
        val cLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        cLp.setMargins(0, 16, 0, 16)
        cv_1.radius = 16F
        cv_1.elevation = 15F
        cv_1.setContentPadding(16, 16, 16, 16)
        lay_0.addView(cv_1, cLp)

        val ll_2 = LinearLayout(ctx)
        ll_2.orientation = LinearLayout.VERTICAL
        val clLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        cv_1.addView(ll_2, clLp)

        val tvTitle = TextView(ctx)
        tvTitle.text = "Урок №$i"
        tvTitle.setTextAppearance(android.R.style.TextAppearance_Material_Body2)

        val tilLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

        val tilName = TextInputLayout(ctx,
            null,
            R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense)
        tilName.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        tilName.boxBackgroundColor = resources.getColor(R.color.design_default_color_secondary)
        tilName.setBoxCornerRadii(5f, 5f, 5f, 5f)
        val etName = TextInputEditText(tilName.context)
        etName.setBackgroundColor(resources.getColor(R.color.fui_transparent))
        etName.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        etName.isSingleLine = true
        tilName.hint = "Название"
        tilName.addView(etName)

        val tilCab = TextInputLayout(ctx,
            null,
            R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense)
        tilCab.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        tilCab.boxBackgroundColor = resources.getColor(R.color.design_default_color_secondary)
        tilCab.setBoxCornerRadii(5f, 5f, 5f, 5f)
        val etCab = TextInputEditText(tilCab.context)
        etCab.setBackgroundColor(resources.getColor(R.color.fui_transparent))
        etCab.inputType = InputType.TYPE_CLASS_NUMBER
        etCab.isSingleLine = true
        etCab.hint = "Кабинет"
        tilCab.addView(etCab)

        ll_2.addView(tvTitle, tilLp)
        ll_2.addView(tilName, tilLp)
        ll_2.addView(tilCab, tilLp)

        lessons.add(arrayListOf(etName, etCab))

        etCab.setText("405")
        etName.setText("Название")
        etOffset.setText("0")

    }

}

package well.keepitsimple.dnevnik.ui.groups.vpPages_class

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.cardview.widget.CardView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import well.keepitsimple.dnevnik.R

class FragmentCreateTimetablePage : Fragment() {

    val lessonsCount = (6..7).random()

    lateinit var lay_0: LinearLayout
    lateinit var etOffset: EditText

    val lessons = ArrayList<ArrayList<View>>()

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

        val ctx: Context = ContextThemeWrapper(
            requireActivity().baseContext,
            R.style.Theme_MaterialComponents_Light
        )

        val cLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        cLp.setMargins(0, 16, 0, 16)

        val cv_1 = CardView(ctx).apply {
            radius = 16F
            elevation = 15F
            setContentPadding(16, 16, 16, 16)
        }

        lay_0.addView(cv_1, cLp)

        val ll_2 = LinearLayout(ctx)
        ll_2.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        cv_1.addView(ll_2, lp)

        val tvTitle = TextView(ctx)
        tvTitle.text = "Урок №$i"
        tvTitle.setTextAppearance(android.R.style.TextAppearance_Material_Body2)

        val etName = TextInputEditText(ctx).apply {
            setBackgroundColor(resources.getColor(R.color.fui_transparent))
            inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            isSingleLine = true
        }
        val tilName = TextInputLayout(
            ctx,
            null,
            R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense
        ).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = resources.getColor(R.color.design_default_color_secondary)
            setBoxCornerRadii(5f, 5f, 5f, 5f)
            hint = "Название"
            addView(etName)
        }

        val etCab = TextInputEditText(ctx).apply {
            setBackgroundColor(resources.getColor(R.color.fui_transparent))
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
            hint = "Кабинет"
        }
        val tilCab = TextInputLayout(
            ctx,
            null,
            R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense
        ).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = resources.getColor(R.color.design_default_color_secondary)
            setBoxCornerRadii(5f, 5f, 5f, 5f)
            addView(etCab)
        }

        ll_2.addView(tvTitle, lp)
        ll_2.addView(tilName, lp)
        ll_2.addView(tilCab, lp)
        val tvGroupNumberLabel = MaterialTextView(ctx)
        tvGroupNumberLabel.text = "Номер группы"
        ll_2.addView(tvGroupNumberLabel)

        val clGroup = LinearLayout(ctx)
        clGroup.orientation = LinearLayout.HORIZONTAL

        ll_2.addView(clGroup, lp)

        var groupCount = 0
        val tvGroupNumber = MaterialTextView(ctx).apply {
            text = "0"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        val minus = ImageButton(ctx).apply {
            setImageDrawable(resources.getDrawable(R.drawable.ic_remove))
            setColorFilter(R.color.design_default_color_secondary)
            isEnabled = false
            setOnClickListener {
                groupCount--
                tvGroupNumber.text = groupCount.toString()
            }
        }
        val plus = ImageButton(ctx).apply {
            setImageDrawable(resources.getDrawable(R.drawable.ic_plus))
            setColorFilter(R.color.design_default_color_secondary)
            setOnClickListener {
                groupCount++
                tvGroupNumber.text = groupCount.toString()
            }
        }

        tvGroupNumber.doAfterTextChanged {
            minus.isEnabled = it.toString().toInt() > 0
        }

        clGroup.apply {
            addView(minus)
            addView(tvGroupNumber)
            addView(plus)
        }

        lessons.add(arrayListOf(etName, etCab, tvGroupNumber, etOffset))

        // TODO: Убрать перед релизом
        /*val rnd = (0..10).random()
        etName.setText(
            when (rnd) {
                0 -> "Русский язык"
                1 -> "Математика"
                2 -> "Английский язык"
                3 -> "Литература"
                4 -> "Физкультура"
                5 -> "География"
                6 -> "ОБЖ"
                7 -> "Информатика"
                8 -> "Экономика"
                else -> "Физикааааа"
            }
        )
        etCab.setText(
            when (rnd) {
                0 -> "405"
                1 -> "405"
                2 -> "309"
                3 -> "405"
                4 -> "1"
                5 -> "405"
                6 -> "405"
                7 -> "408"
                8 -> "405"
                else -> "410"
            }
        )
        etOffset.setText("${(0..3).random()}")*/
    }
}


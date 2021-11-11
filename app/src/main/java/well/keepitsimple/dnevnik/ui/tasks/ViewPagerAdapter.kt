package well.keepitsimple.dnevnik.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.R

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
        PagerVH(LayoutInflater.from(parent.context).inflate(R.layout.create_document_page1, parent, false))

    override fun getItemCount(): Int = 3

    override fun onBindViewHolder(holder: PagerVH, position: Int) = holder.itemView.run {

    }
}

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)
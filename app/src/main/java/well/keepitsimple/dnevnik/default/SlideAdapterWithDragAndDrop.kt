package well.keepitsimple.dnevnik.default

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SlideAdapterWithDragAndDrop(hostFragment: Fragment, _fragments: MutableList<Fragment>): FragmentStateAdapter(hostFragment) {

    private val fragments: MutableList<Fragment> = _fragments

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemCount() = fragments.size


    fun insertItem(f: Fragment) {
        fragments.add(f)
        notifyItemInserted(fragments.size)
    }

    fun removeItem(f: Fragment) {
        var index = 0
        fragments.forEach {
            if (it == f) { fragments.removeAt(index) }
            index++
        }
        notifyItemRemoved(index)
    }

}
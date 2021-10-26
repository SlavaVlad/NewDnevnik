package well.keepitsimple.dnevnik.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.addSwitch

class UsersAdapter(private val users: ArrayList<MainActivity.User>) :
    RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    val checkedUsersID = ArrayList<String>()

    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: CheckBox = itemView.findViewById(R.id.name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_one_line, parent, false)
        return UsersViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {

        holder.name.text = users[position].email

        holder.name.setOnCheckedChangeListener { btn, b ->
            users.forEach {
                it.email
                if (btn.text == it.email) {
                    checkedUsersID.addSwitch(it.uid!!)
                }
            }
        }

    }

    override fun getItemCount() = users.size
}

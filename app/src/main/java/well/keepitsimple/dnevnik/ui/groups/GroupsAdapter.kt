package well.keepitsimple.dnevnik.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group

class GroupsAdapter(private val names: List<Group>) :
    RecyclerView.Adapter<GroupsAdapter.GroupsViewHolder>() {

    class GroupsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var users: TextView? = null
        var type: TextView? = null
        var id: TextView? = null

        init {
            name = itemView.findViewById(R.id.i_group_name)
            users = itemView.findViewById(R.id.i_users)
            type = itemView.findViewById(R.id.i_type)
            id = itemView.findViewById(R.id.i_id)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupsViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.group_item, parent, false)
        return GroupsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GroupsViewHolder, position: Int) {
        names[position].users?.forEach {
            holder.users?.text = "${holder.users?.text}$it\n"
        }

        holder.name?.text = names[position].name
        holder.type?.text = names[position].type
        holder.id?.text = names[position].id

    }

    override fun getItemCount() = names.size
}
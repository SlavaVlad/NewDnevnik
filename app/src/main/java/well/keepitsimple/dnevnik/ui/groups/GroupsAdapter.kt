package well.keepitsimple.dnevnik.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group

class GroupsAdapter(private val names: List<Group>) :
    RecyclerView.Adapter<GroupsAdapter.GroupsViewHolder>() {

    class GroupsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var users: TextView? = null
        var admins: TextView? = null
        var type: TextView? = null
        var id: TextView? = null
        var label_users: TextView? = null
        var label_admins: TextView? = null

        init {
            name = itemView.findViewById(R.id.group_name)
            users = itemView.findViewById(R.id.users)
            admins = itemView.findViewById(R.id.admins)
            type = itemView.findViewById(R.id.type)
            id = itemView.findViewById(R.id.id)
            label_admins = itemView.findViewById(R.id.label_admins)
            label_users = itemView.findViewById(R.id.label_users)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupsViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.group_item, parent, false)
        return GroupsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GroupsViewHolder, position: Int) {

        val item = names[position]

        item.users?.forEach {
            holder.users?.isVisible = true
            holder.label_users?.isVisible = true
            holder.users?.text = it
        }

        item.admins?.forEach {
            holder.admins?.isVisible = true
            holder.label_admins?.isVisible = true
            holder.admins?.text = it.key
        }

        holder.name !!.text = item.name
        holder.type !!.text = item.type
        holder.id !!.text = item.id

    }

    override fun getItemCount() = names.size
}
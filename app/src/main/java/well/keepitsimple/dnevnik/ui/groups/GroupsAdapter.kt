package well.keepitsimple.dnevnik.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group

class GroupsAdapter(private val groups: List<Group>, private val onClickListener: GroupOnClickListener) :
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
            name = itemView.findViewById(R.id.tv_group_name)
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

    override fun onBindViewHolder(h: GroupsViewHolder, position: Int) {

        val item = groups[position]

        if (item.users != null) {
            var users = ""
            item.users ?.forEach {
                users = "${users}${it}\n"
            }
            h.users?.isVisible = true
            h.users?.text = users
            h.label_users?.isVisible = true
        }
        if (item.admins != emptyMap<String, Any>()) {
            var admins = ""
            item.admins?.forEach {
                admins = "${admins}${it.key}\n"
            }
            h.admins?.isVisible = true
            h.admins?.text = admins
            h.label_admins?.isVisible = true
        }
        h.name ?.text = item.name
        h.type !!.text = item.type
        h.id !!.text = item.id

        h.itemView.setOnClickListener {
            onClickListener.onClick(groups[position])
        }
    }

    override fun getItemCount() = groups.size
}
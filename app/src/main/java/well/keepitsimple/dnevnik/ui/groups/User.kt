package well.keepitsimple.dnevnik.ui.groups

import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.login.Group

class User(
    var uid: String? = null,
    val groups: ArrayList<Group> = ArrayList(),
) {

    val db = FirebaseFirestore.getInstance()

    fun getGroupByType(type: String): Group {
        groups.forEach {
            if (it.type == type) {
                return it
            }
        }
        return Group()
    }

    fun isAllowedInGroup(permission: String, group: Group): Boolean {
        if (group.rights!!.contains(permission)) {
            return true
        } else if (group.admins!!.contains(this.uid)) {
            if (group.admins!![this.uid]!!.contains(permission)) {
                return true
            }
        }
        return false
    }

    fun isAllowedAsAdmin(permission: String, group: Group): Boolean {
        return group.admins!![this.uid]!!.contains(permission)
    }

    fun getGroupsWhereAdmin(): List<Group> {
        val toReturn = mutableListOf<Group>()

        groups.forEach {
            if (it.admins!!.contains(uid)) {
                toReturn.add(it)
            }
        }

        return toReturn
    }

    @Deprecated("Deprecated in class Groups system", ReplaceWith("getPermissionsByGroup()"))
    fun isAllow(p: String): Boolean {
        return this.getAllPermissions().contains(p)
    }

    @Deprecated("Deprecated in class Groups system", ReplaceWith("getPermissionsByGroup()"))
    fun getAllPermissions(): ArrayList<String> {
        val permissions = ArrayList<String>()
        repeat(groups.size) { groupIndex ->
            repeat(groups[groupIndex].rights!!.size) { rightIndex ->
                permissions.add(groups[groupIndex].rights!![rightIndex])
            }
        }
        return permissions
    }

}
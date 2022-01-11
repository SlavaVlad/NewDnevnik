package well.keepitsimple.dnevnik.ui.groups

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.login.Group
import kotlin.coroutines.CoroutineContext

open class User() : CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    var groupsUser: MutableList<Group> = mutableListOf()
    var groupsAdmin: MutableList<Group> = mutableListOf()

    var uid: String = ""

    fun setUserID(set: String) {
        if (uid != set) {
            uid = set
        }
    }

    fun getGroupByType(type: String): Group {
        groupsUser.forEach {
            if (it.type == type) {
                return it
            }
        }
        return Group()
    }

    fun getGroupsByPermission(permission: String, flags: List<MemberTypes>): List<Group> {
        val returnIt = mutableListOf<Group>()
        if (flags.contains(MemberTypes.ADMIN)) {
            returnIt += groupsUser.filter { it.rights!!.contains(permission) }
        }
        if (flags.contains(MemberTypes.USER)) {
            returnIt += groupsAdmin.filter { it.admins!![uid]!!.contains(permission) }
        }
        return returnIt.distinctBy {
            it.id
        }
    }

    fun getClassRef(): DocumentReference {
        val db = FirebaseFirestore.getInstance()
        return db.collection("group")
            .document(getGroupByType("school").id!!)
            .collection("groups")
            .document(getGroupByType("class").id!!)
    }

    fun getSchoolRef(): DocumentReference {
        val db = FirebaseFirestore.getInstance()
        return db.collection("groups")
            .document(getGroupByType("school").id!!)
    }

    fun checkGroupById(id: String): Boolean {
        val ids = mutableListOf<String>()
        groupsUser.forEach {
            ids.add(it.id!!)
        }
        return ids.contains(id)
    }

    fun isAllowedInGroup(permission: String, group: Group): Boolean {
        group
        if (group.rights!!.contains(permission)) {
            return true
        } else if (group.admins!!.contains(this.uid)) {
            if (group.admins!![this.uid]!!.contains(permission)) {
                return true
            }
        }
        return false
    }

    @Deprecated("Deprecated in class Groups system", ReplaceWith("getPermissionsByGroup()"))
    fun getAllPermissions(): ArrayList<String> {
        val permissions = ArrayList<String>()
        repeat(groupsUser.size) { groupIndex ->
            repeat(groupsUser[groupIndex].rights!!.size) { rightIndex ->
                permissions.add(groupsUser[groupIndex].rights!![rightIndex])
            }
        }
        return permissions
    }

}
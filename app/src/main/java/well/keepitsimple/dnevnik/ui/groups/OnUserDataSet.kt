package well.keepitsimple.dnevnik.ui.groups

import well.keepitsimple.dnevnik.login.Group

interface OnUserDataSet {
    fun onDataSet(uid: String, groupsAdmin: List<Group>, groupsUser: List<Group>)
}
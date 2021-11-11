package well.keepitsimple.dnevnik.login

sealed class Rights {

    enum class Doc(val r: String) {
        VIEW("docView"),
        EDIT("docEdit"),
        CREATE("docCreate"),
        DELETE("docDelete"),
        COMPLETE("docComplete"),
        ADDFILES("docAddFiles"),
    }

    enum class Lesson(val r: String) {
        VIEW("lessonsView"),
        EDIT("lessonsEdit"),
        CREATE("lessonsCreate"),
        DELETE("lessonsDelete"),
    }

    enum class Group(val r: String) {
        VIEW("groupsView"),
        EDIT("groupsEdit"),
        CREATE("groupsCreate"),
        DELETE("groupsDelete"),
    }

}
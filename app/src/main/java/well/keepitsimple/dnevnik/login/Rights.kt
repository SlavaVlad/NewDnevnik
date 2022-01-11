package well.keepitsimple.dnevnik.login

abstract class Rights {

    enum class Doc(val string: String) {
        VIEW("docView"),
        EDIT("docEdit"),
        CREATE("docCreate"),
        DELETE("docDelete"),
        COMPLETE("docComplete"),
        ADDFILES("docAddFiles"),
    }

    enum class Lesson(val string: String) {
        VIEW("lessonsView"),
        EDIT("lessonsEdit"),
        CREATE("lessonsCreate"),
        DELETE("lessonsDelete"),
    }

    enum class Group(val string: String) {
        VIEW("groupsView"),
        EDIT("groupsEdit"),
        CREATE("groupsCreate"),
        DELETE("groupsDelete"),
    }

}
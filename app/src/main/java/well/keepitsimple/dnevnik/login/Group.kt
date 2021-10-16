package well.keepitsimple.dnevnik.login

class Group(
    var name: String? = null,
    var rights: List<String>? = null,
    var type: String? = null,
    var parent: String? = null,
)

//docView - просмотр домашек
//docCreate - создание домашки
//docEdit - редактирование домашки
//docDelete - удаление домашек

//docComplete - завершать домашки
//docAddFiles - прикреплять файлы к домашкам


//lessonsView - просмотр уроков
//lessonsCreate - создание уроков
//lessonsEdit - редактирование уроков
//lessonsDelete - удаление уроков


//groupsView - просмотр групп школы
//groupsCreate - создание групп школы
//groupsEdit - редактирование групп школы
//groupsDelete - удаление групп школы
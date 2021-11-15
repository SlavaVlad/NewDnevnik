package well.keepitsimple.dnevnik.login

import com.google.firebase.firestore.DocumentSnapshot

class Group(
    var name: String? = null, // Название группы
    var rights: List<String>? = null, // Права, которые даёт эта группа всем, кто в ней состоит
    var type: String? = null, // Тип (Школа или класс)
    var id: String? = null, // ИД группы (привязан к ИД документа)
    var users: List<String>? = null, // Список пользователей, состоящих в этой группе
    var admins: HashMap<String, List<String>>? = null, // ИД пользователей админов // Права админов на эту группу
    var doc: DocumentSnapshot? = null,
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
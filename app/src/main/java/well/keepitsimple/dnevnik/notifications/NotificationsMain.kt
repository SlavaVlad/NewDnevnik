package well.keepitsimple.dnevnik.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import well.keepitsimple.dnevnik.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.addUnique
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import java.util.Calendar.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


const val NEW_TASKS = "Новые домашние задания"
const val NEXT_LESSON = "Следующий урок"
const val DEADLINE = "Домашние задания на завтра"

class NotificationsMain : Service(), CoroutineScope {

    override fun onBind(p0: Intent?): IBinder? = null

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val db = FirebaseFirestore.getInstance()

    val notify_hour_deadline = 22
    val notify_min_deadline = 56

    var uid = ""
    var user: MainActivity.User = MainActivity.User()
    val list_lessons = ArrayList<Lesson>()

    @ExperimentalTime
    override fun onCreate() {
        super.onCreate()
        uid = loadText("uid").toString()

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            user = userDoc.toObject<MainActivity.User>() !!
            getRights()
        }

    }

    @ExperimentalTime
    private fun getRights() {
        db
            .collectionGroup("groups")
            .whereArrayContains("users", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->

                Log.d("Service", "GROUPS: ${querySnapshot.documents.size}")

                var index = 0
                querySnapshot.documents.forEach {   // записываем группы в пользователя
                    user.groups.add(it.toObject<Group>() !!)
                    user.groups[index].id = it.id
                    index ++
                }

                getTimetables()
                setNotification_NewTask()
                launch{ await_day_setNotification_DeadlineIsNear(notify_hour_deadline, notify_min_deadline) }

            }
    }

    @ExperimentalTime
    private fun getTimetables() {
        user.getGroupByType("school").id !!
        db.collection("lessonstime")
            .document("LxTrsAIg81E96zMSg0SL")
            .get()
            .addOnSuccessListener { lesson_time -> // расписание звонков
                db.collection("lessonsgroups")
                    .document(user.getGroupByType("school").id !!) // NPE
                    .collection("lessons")
                    .whereEqualTo("class", user.getGroupByType("class").id !!) // хдета тут проблема
                    .get()
                    .addOnSuccessListener { lesson_query -> // получаем всё расписание на все дни
                        lesson_query.forEach { // проходимся по каждому документу
                            val lesson = it // получаем текущий документ
                            var error = 0
                            repeat(
                                lesson.getLong("lessonsCount") !!
                                    .toInt()
                            ) { loop -> // проходимся по списку уроков в дне
                                val loopindex: Int = loop + 1 // получаем не 0-based индекс
                                var offset = lesson.getLong("timeOffset") !!
                                when (lesson.get("${loopindex}_parent")) {
                                    null -> {
                                        offset -= error
                                        list_lessons.add(
                                            Lesson( // добавляем урок в массив
                                                cab = lesson.getLong("${loopindex}_cab") !!, // кабинет
                                                name = lesson.getString("${loopindex}_name") !!, // название предмета (пример: Математика)
                                                startAt = lesson_time.getString("${loopindex + offset}_startAt") !!, // время начала урока 09:15, например
                                                endAt = lesson_time.getString("${loopindex + offset}_endAt") !!, // время конца урока
                                                day = lesson.getLong("day") !! // номер дня в неделе, ПН=1, СБ=6
                                            )
                                        )
                                    }
                                    else -> {
                                        if (lesson.getBoolean("${loopindex}_parent") !!) {
                                            offset -= error
                                            list_lessons.add(
                                                Lesson( // добавляем урок в массив
                                                    cab = lesson.getLong("${loopindex}_cab") !!, // кабинет
                                                    name = lesson.getString("${loopindex}_name") !!, // название предмета (пример Математика)
                                                    startAt = lesson_time.getString("${loopindex + offset}_startAt") !!, // время начала урока 09:15, например
                                                    endAt = lesson_time.getString("${loopindex + offset}_endAt") !!, // время конца урока
                                                    day = lesson.getLong("day") !!
                                                )
                                            ) // номер дня в неделе, ПН=1, СБ=6))
                                        } else {
                                            error ++
                                            offset -= error
                                            list_lessons.add(
                                                Lesson( // добавляем урок в массив
                                                    cab = lesson.getLong("${loopindex}_cab") !!, // кабинет
                                                    name = lesson.getString("${loopindex}_name") !!, // название предмета (пример Математика)
                                                    startAt = lesson_time.getString("${loopindex + offset}_startAt") !!, // время начала урока 09:15, например
                                                    endAt = lesson_time.getString("${loopindex + offset}_endAt") !!, // время конца урока
                                                    day = lesson.getLong("day") !! // номер дня в неделе, ПН=1, СБ=6
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        list_lessons.sortByDescending { list_lessons -> list_lessons.day }
                        launch { setNotification_NextLesson() }
                    }
            }
    }

    private fun setNotification_NewTask() {
        db.collection("6tasks")
            .whereEqualTo("school", user.getGroupByType("school").id)
            .whereEqualTo("class", user.getGroupByType("class").id)
            .whereNotEqualTo("complete", uid)
            .addSnapshotListener { value, error ->
                Log.e("Service", "startNotify: ${value !!.documents.size}")
                value.documentChanges.forEach {
                    if (it.type == DocumentChange.Type.ADDED && it.document.getString("owner") != uid) {
                        notify_NewTask("Новое Д/з!",
                            "Добавлено задание по предмету " +
                                    "${it.document.getString("subject")}",
                            "${it.document.getString("text")}",
                            R.drawable.ic_school,
                            NotificationManager.IMPORTANCE_DEFAULT,
                            1,
                            NEW_TASKS,
                            uid,
                            it.document.id
                        )
                    }
                }
            }
    }
    private fun notify_NewTask(title: String, text: String, bigText: String, icon: Int, priority: Int, id: Int, channelId: String, uid: String, docUid: String, ) {

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentAction = PendingIntent.getActivity(this, 0, contentIntent, 0)

        val buttonIntent = Intent(this, NotificationActionReceiver::class.java)
            .putExtra("uid", uid)
            .putExtra("docUid", docUid)
        val buttonAction =
            PendingIntent.getBroadcast(this, 0, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text + "Его текст: " + bigText)
            .setAutoCancel(true)
            .setPriority(priority)
            .addAction(R.drawable.ic_ok_img, "Выполнено", buttonAction)
            .setContentIntent(contentAction)

        val notificationChannel =
            NotificationChannel(channelId, channelId, priority)
        NotificationManagerCompat.from(this).createNotificationChannel(notificationChannel)

        with(NotificationManagerCompat.from(this)) {
            this.notify(id, builder.build()) // посылаем уведомление
        }
    }

    @ExperimentalTime
    private suspend fun await_day_setNotification_DeadlineIsNear(h: Int, m: Int) {
        val c = getInstance(TimeZone.getDefault())
        if ((c.get(DAY_OF_WEEK)-1) != 7 && c.get(HOUR_OF_DAY) == h && c.get(MINUTE) == m) {
            setNotification_DeadlineIsNear()
            delay(Duration.hours(23.5))
            await_day_setNotification_DeadlineIsNear(notify_hour_deadline, notify_min_deadline)
        } else {
            delay(Duration.seconds(1))
            await_day_setNotification_DeadlineIsNear(notify_hour_deadline, notify_min_deadline)
        }
    }

    private fun setNotification_DeadlineIsNear() {
        db.collection("6tasks")
            .whereEqualTo("school", user.getGroupByType("school").id)
            .whereEqualTo("class", user.getGroupByType("class").id)
            .whereNotEqualTo("complete", uid)
            .orderBy("complete")
            .orderBy("deadline", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { query ->
                val docs: ArrayList<DocumentSnapshot> = ArrayList()
                query.documents.forEach {
                    if (getDeadlineInDays(it.getTimestamp("deadline")) == 1.0) {
                        docs.add(it)
                    }
                }
                if (docs.size > 0) {
                    var text = ""
                    docs.forEach {
                        text += ("\n${it.getString("subject")}, ")
                    }
                    text.substring(text.length - 2, text.length)
                    notify_DeadlineIsNear("Д/з на завтра",
                        "Не сделаны Д/з по предметам: $text",
                        R.drawable.ic_clock,
                        NotificationManager.IMPORTANCE_HIGH,
                        3,
                        DEADLINE)
                }
            }
    }
    private fun notify_DeadlineIsNear(title: String, text: String, icon: Int, priority: Int, id: Int, channelId: String, ) {
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentAction = PendingIntent.getActivity(this, 0, contentIntent, 0)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(priority)
            .setContentIntent(contentAction)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))

        val notificationChannel =
            NotificationChannel(channelId, channelId, priority)
        NotificationManagerCompat.from(this).createNotificationChannel(notificationChannel)

        with(NotificationManagerCompat.from(this)) {
            this.notify(id, builder.build()) // посылаем уведомление
        }
    }

    @ExperimentalTime
    private suspend fun setNotification_NextLesson() {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        var index = 0
        list_lessons.forEach {
            if (it.day.toInt() == (calendar.get(DAY_OF_WEEK) - 1)) {
                val t = it.endAt
                val h = (t[0].toString() + t[1].toString()).toInt()
                val m = (t[3].toString() + t[4].toString()).toInt()
                if (calendar.get(HOUR_OF_DAY) == h && calendar.get(MINUTE) == m) {
                    val les = list_lessons[index + 1]
                    if (les.day.toInt() == (calendar.get(DAY_OF_WEEK) - 1)) {
                        notify_NextLesson(
                            "Следующий урок ",
                            "${les.name} в кабинете № ${les.cab}",
                            R.drawable.ic_school,
                            NotificationManager.IMPORTANCE_DEFAULT,
                            2,
                            NEXT_LESSON
                        )
                    }
                }
                Log.d("Service", "setNotification_NextLesson: check ${it.day}")
            }
            index ++
        }
        index = 0
        delay(Duration.seconds(30))
        Log.d("Service", "setNotification_NextLesson: checkNearLesson")
        setNotification_NextLesson()
    }
    private fun notify_NextLesson(title: String, text: String, icon: Int, priority: Int, id: Int, channelId: String, ) {

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(priority)

        val notificationChannel =
            NotificationChannel(channelId, channelId, priority)
        NotificationManagerCompat.from(this).createNotificationChannel(notificationChannel)

        with(NotificationManagerCompat.from(this)) {
            this.notify(id, builder.build()) // посылаем уведомление
        }
    }

    private fun loadText(key: String): String? {
        val sPref = getSharedPreferences("uid", MODE_PRIVATE)
        return sPref.getString(key, "")
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp !!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("Service", "onDestroy: service destroyed")
    }

}
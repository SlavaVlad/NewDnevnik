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
import androidx.work.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import java.util.Calendar.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

const val NEW_TASKS = "Добавлено задание"
const val NEXT_LESSON = "Следующий урок"

class NotificationsMainService : Service(), CoroutineScope {

    val TAG = "Service"

    override fun onBind(p0: Intent?): IBinder? = null

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val db = FirebaseFirestore.getInstance()

    private var notify_min_deadline: Int = 30
    private var notify_hour_deadline: Int = 16

    var uid = ""
    var user: MainActivity.User = MainActivity.User()
    private val list_lessons = ArrayList<Lesson>()

    @ExperimentalTime
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent !!.extras != null) {
            if (intent.extras !!.containsKey("settingsUpdate")) {
                when (intent.extras?.getString("settingsUpdate")) {
                    "btnDeadlineIsNear_hour" -> {
                        notify_hour_deadline = loadInt("btnDeadlineIsNear_hour", 16)
                    }
                    "btnDeadlineIsNear_minute" -> {
                        notify_min_deadline = loadInt("btnDeadlineIsNear_minute", 30)
                    }
                }
            }
            if (intent.extras !!.containsKey("uid")) {
                uid = intent.extras !!.getString("uid") !!
            }
        }

        if (loadText("uid") != "") {
            uid = loadText("uid")
        } else {
            Log.e(TAG, "onStartCommand: uid is not found")
        }
        notify_min_deadline = loadInt("btnDeadlineIsNear_minute", 30)
        notify_hour_deadline = loadInt("btnDeadlineIsNear_hour", 16)

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            user = userDoc.toObject<MainActivity.User>() !!
            getRights()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @ExperimentalTime
    fun getRights() {
        db
            .collectionGroup("groups")
            .whereArrayContains("users", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->

                Log.d(TAG, "GROUPS: ${querySnapshot.documents.size}")

                var index = 0
                querySnapshot.documents.forEach {   // записываем группы в пользователя
                    user.groups.add(it.toObject<Group>() !!)
                    user.groups[index].id = it.id
                    index ++
                }

                getTimetables()
                setNotification_NewTask()
                setNotification_DeadlineIsNear()

            }
    }

    @ExperimentalTime
    private fun setNotification_DeadlineIsNear() {

        val data = Data
            .Builder()
            .putString("uid", uid)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val nowh = calendar.get(HOUR_OF_DAY)
        val nowm = calendar.get(MINUTE)

        WorkManager.getInstance().cancelAllWorkByTag("notificationDeadline")

        val notificationDeadline = PeriodicWorkRequest.Builder(NotifyDeadlineWorker::class.java,
            1438,
            TimeUnit.MINUTES,
            1442,
            TimeUnit.MINUTES)
            .setInitialDelay(java.time.Duration.ofMinutes(abs(nowh - notify_hour_deadline).toLong() + abs(
                nowm - notify_min_deadline).toLong()))
            .setInputData(data)
            .addTag("notificationDeadline")
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance().enqueueUniquePeriodicWork("notificationDeadline",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationDeadline)
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
        db.collection("groups")
            .document(user.getGroupByType("school").id !!)
            .collection("groups")
            .document(user.getGroupByType("class").id !!)
            .collection("tasks")
            .whereNotEqualTo("completed", user.uid)
            .addSnapshotListener { value, error ->
                Log.e(TAG, "startNotify: ${value !!.documents.size}")
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

    private fun notify_NewTask(
        title: String,
        text: String,
        bigText: String,
        icon: Int,
        priority: Int,
        id: Int,
        channelId: String,
        uid: String,
        docUid: String,
    ) {

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentAction = PendingIntent.getActivity(this, 0, contentIntent, 0)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text + "Его текст: " + bigText)
            .setAutoCancel(true)
            .setPriority(priority)
            .setContentIntent(contentAction)

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
                Log.d(TAG, "setNotification_NextLesson: check ${it.day}")
            }
            index ++
        }
        index = 0
        delay(Duration.seconds(30))
        Log.d(TAG, "setNotification_NextLesson: checkNearLesson")
        setNotification_NextLesson()
    }

    private fun notify_NextLesson(
        title: String,
        text: String,
        icon: Int,
        priority: Int,
        id: Int,
        channelId: String,
    ) {

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

    private fun loadText(key: String): String {
        val sPref = getSharedPreferences("uid", MODE_PRIVATE)
        return sPref.getString(key, "") !!
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp !!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

    fun loadInt(key: String, defValue: Int): Int {
        val sPref = getSharedPreferences("uid", MODE_PRIVATE)
        return sPref.getInt(key, defValue)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: service destroyed")
    }

}
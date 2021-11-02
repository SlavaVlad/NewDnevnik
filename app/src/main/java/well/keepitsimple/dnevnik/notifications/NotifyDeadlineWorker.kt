package well.keepitsimple.dnevnik.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.MainActivity
import well.keepitsimple.dnevnik.R
import well.keepitsimple.dnevnik.addUnique
import well.keepitsimple.dnevnik.login.Group
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.time.ExperimentalTime

const val DEADLINE = "Домашние задания на завтра"

class NotifyDeadlineWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams), CoroutineScope {

    val TAG = "Worker"

    var ctx: Context? = null

    var user: MainActivity.User = MainActivity.User()

    val db = FirebaseFirestore.getInstance()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @ExperimentalTime
    override fun doWork(): Result {

        Log.d(TAG, "doWork: $DEADLINE")

        val uid = inputData.getString("uid") !!

        ctx = applicationContext

        getUser(uid)

        return Result.success()
    }

    @ExperimentalTime
    private fun getUser(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            user = userDoc.toObject<MainActivity.User>() !!
            user.uid = userDoc.id
            getRights()
        }.addOnFailureListener {
            Log.e(TAG, "getUser: $it")
        }
    }

    @ExperimentalTime
    fun getRights() {
        db.collectionGroup("groups")
            .whereArrayContains("users", user.uid!!)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var index = 0
                querySnapshot.documents.forEach {   // записываем группы в пользователя
                    user.groups.add(it.toObject<Group>() !!)
                    user.groups[index].id = it.id
                    index ++
                }
                setNotification_DeadlineIsNear()
            }.addOnFailureListener {
                Log.e(TAG, "getUser: $it")
            }
    }

    private fun setNotification_DeadlineIsNear() {
        db.collection("6tasks")
            .whereEqualTo("school", user.getGroupByType("school").id)
            .whereEqualTo("class", user.getGroupByType("class").id)
            .whereNotEqualTo("complete", user.uid)
            .get()
            .addOnSuccessListener { query ->

                var txt = ""
                val names: ArrayList<String> = ArrayList()
                query.documents.forEach {
                    if (getDeadlineInDays(it.getTimestamp("deadline")) == 1.0) {
                        names.addUnique(it.getString("subject") !!)
                    }
                }

                if (names.size > 0) {
                    names.forEach {
                        txt += ("$it,\n")
                    }
                    txt = txt.substring(0, txt.length - 2)
                }

                if (txt.isNotEmpty()) {
                    notify_DeadlineIsNear(txt)
                }

            }.addOnFailureListener {
                Log.e(TAG, "getUser: $it")
            }

    }

    private fun notify_DeadlineIsNear(text: String) {
        val contentIntent = Intent(ctx, MainActivity::class.java)
        val contentAction = PendingIntent.getActivity(ctx, 0, contentIntent, 0)

        val builder = NotificationCompat.Builder(ctx !!, DEADLINE)
            .setSmallIcon(R.drawable.ic_school)
            .setContentTitle("Не сделаны Д/з на завтра по предметам:")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setContentIntent(contentAction)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))

        val notificationChannel =
            NotificationChannel(DEADLINE, DEADLINE, NotificationManager.IMPORTANCE_HIGH)
        NotificationManagerCompat.from(ctx !!).createNotificationChannel(notificationChannel)

        with(NotificationManagerCompat.from(ctx !!)) {
            this.notify(3, builder.build()) // посылаем уведомление
        }
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp !!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

}
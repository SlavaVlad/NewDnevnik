package well.keepitsimple.dnevnik

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class NotifyDeadlineTasks(context: Context, parameters: WorkerParameters): CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo())
        return@withContext runCatching {
            createForegroundInfo()
            Result.success()
        }.getOrElse {
            Result.failure()
        }
    }

    //Creates notifications for service
    private fun createForegroundInfo(): ForegroundInfo {
        var notification: Notification? = null
        val tasks = ArrayList<String>()
        db.collection("tasks").orderBy("deadline").get()
            //.whereEqualTo("group", "-")
            .addOnSuccessListener {
                for (doc in it.documents.iterator()) { // проходим по каждому документу
                    if (getDeadlineInDays(doc.getTimestamp("deadline")) > 0) {
                        tasks.add(doc.getString("text")+"\n").toString()
                    }
                }

                val id = "11"
                val channelName = "Сообщение о сдаче"
                val title = "Завтра сдача: "
                val text = tasks.toString()

                createChannel(id, channelName)

                notification = NotificationCompat.Builder(applicationContext, id)
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_school)
                    .setContentText(text)
                    .build()

            }

        return ForegroundInfo(11, notification!!)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String, channelName: String) {
        notificationManager.createNotificationChannel(
            NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp!!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }
}
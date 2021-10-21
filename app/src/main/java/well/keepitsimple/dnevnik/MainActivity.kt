package well.keepitsimple.dnevnik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.core.OrderBy
import com.google.firebase.firestore.ktx.toObject
import com.onesignal.OneSignal
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.tasks.TaskItem
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.DAY_OF_WEEK
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

const val ONESIGNAL_APP_ID = "b5aa6c76-4619-4497-9b1e-2e7a1ef4095f"
const val DAY_S = 86400
const val WEEK = 7

class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN: Int = 123
    val F: String = "Firebase"

    var uid: String? = null
    var user: User = User()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val db = FirebaseFirestore.getInstance()
    var list_lessons = ArrayList<Lesson>()
    val tasks = ArrayList<TaskItem>()

    private var job: Job = Job()

    val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Logging set to help debug issues, remove before releasing your app.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)

        // OneSignal Initialization
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)

        // Configure Google Sign In
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tasks, R.id.nav_lk, R.id.nav_settings, R.id.nav_timetables, R.id.nav_groups
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    // Взаимодействие с фрагментами

    private fun getTimetables() {
        val tmp_list_lessons = ArrayList<Lesson>()
        // запрос документов расписания
        db
            .collection("lessonstime")
            .document("LxTrsAIg81E96zMSg0SL")
            .get()
            .addOnSuccessListener { lesson_time -> // расписание звонков
                db
                    .collection("lessonsgroups")
                    .document(user.getGroupByType("school").id!!)
                    .collection("lessons")
                    .whereEqualTo("class", user.getGroupByType("class").id !!)
                    .get()
                    .addOnSuccessListener { lesson_query -> // получаем всё расписание на все дни
                        lesson_query.forEach { // проходимся по каждому документу
                            Log.d("TEST", it.getLong("day").toString())
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
                    }

                list_lessons.sortByDescending { list_lessons -> list_lessons.day }

                Log.d("TEST", list_lessons.toString())

            }
    } // Получили расписание

    fun getNextLesson(lesson_name: String): Long {

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val today = calendar.get(DAY_OF_WEEK) - 1
        val now = (System.currentTimeMillis() / 1000)
        val tmpLessons = ArrayList<Lesson>()

        list_lessons.forEach {
            if (it.day > today) {
                val tmp = Lesson(it.cab, it.name, it.startAt, it.endAt, it.day)
                val l = tmp.day - today
                tmpLessons.add(tmp)
            }
        }

        list_lessons.forEach {
            val tmp = Lesson(it.cab, it.name, it.startAt, it.endAt, it.day)
            tmp.day += 7 - today
            tmpLessons.add(tmp)
        }

        Log.d("DEBUG", tmpLessons.toString())

        // ищем совпадающий по имени предмет в списке и возвращаем его
        tmpLessons.forEach {
            if (lesson_name == it.name) {

                Log.d("DEBUG", "Today: ${calendar.get(DAY_OF_MONTH)}")
                Log.d("DEBUG", "Selected: $it")
                Log.d("DEBUG", "Returned: ${(now + ((it.day) * DAY_S))}")

                return (now + ((it.day) * DAY_S))
            }
        }

        return 1
    }

    // Конец взаимодействия с фрагментами


    // Сервисные глобальные методы
    fun alert(title: String, message: String, source: String) {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Ok") { dialog, id ->
            }
            .show()
        Log.e("ErrorAlert", "t: $title m: $message")
        Log.e("ErrorAlert", source)
    }


    // START LOGIN
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            signIn()
        } else {
            checkUserInDatabase(currentUser)
            uid = currentUser.uid
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(F, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken !!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(F, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(F, "signInWithCredential:success")
                    val user = auth.currentUser !!
                    uid = auth.currentUser !!.uid

                    checkUserInDatabase(user)

                } else {
                    alert(
                        "Ошибка входа!",
                        "Дальнейшая работа приложения невозможна по причине: ${task.exception}",
                        "firebaseAuthWithGoogle"
                    )
                }
            }
    }

    class User(
        val email: String? = null,
        val groups: ArrayList<Group> = ArrayList()
    ) {

        fun getGroupByType(type: String): Group { // TODO: сделать типы групп перечислением

            groups.forEach {
                if (it.type == type) {
                    return it
                }
            }

            return Group()
        }

        fun checkPermission(p: String): Boolean {
            return this.getAllPermissions().contains(p)
        }

        fun getAllPermissions(): ArrayList<String> {
            val permissions = ArrayList<String>()
            repeat(groups.size) { groupIndex ->
                repeat(groups[groupIndex].rights !!.size) { rightIndex ->
                    permissions.add(groups[groupIndex].rights !![rightIndex])
                }
            }
            return permissions
        }

    }

    private fun checkUserInDatabase(fire_user: FirebaseUser) {
        db.collection("users").document(fire_user.uid).get().addOnSuccessListener {
            if (it.exists()) {

                user = it.toObject<User>() !!

                getRights()

            } else {

                val data = hashMapOf<String, Any>(
                    "email" to fire_user.email !!
                )

                val docRef = db.collection("users").document(fire_user.uid)
                docRef.set(data).addOnSuccessListener {
                    Log.w(F, "User data write successfully")
                }.addOnFailureListener { e ->
                    Log.w(F, "Error writing user data - ${e.message}")
                }

            }
        }
    }

    private fun getRights() {
        db
            .collectionGroup("groups")
            .whereArrayContains("users", uid!!)
            .get()
            .addOnSuccessListener { querySnapshot ->

                Log.d("TEST", "GROUPS: ${querySnapshot.documents.size}")

                var index = 0

                querySnapshot.documents.forEach {   // записываем группы в пользователя
                    user.groups.add(it.toObject<Group>() !!)
                    user.groups[index].id = it.id
                    Log.w(F, user.groups.toString())
                    index ++
                }

                getTimetables()

            }
    }

    // END LOGIN


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

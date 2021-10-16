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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.onesignal.OneSignal
import kotlinx.coroutines.*
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.tasks.TaskItem
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.sql.Date
import java.util.*
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.DAY_OF_WEEK
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

const val ONESIGNAL_APP_ID = "b5aa6c76-4619-4497-9b1e-2e7a1ef4095f"
const val DAY_IN_SECONDS = 86400

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
    val list_lessons = ArrayList<Lesson>()
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
                R.id.nav_tasks, R.id.nav_lk, R.id.nav_settings, R.id.nav_timetables
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        getTimetables()

    }

    // Взаимодействие с фрагментами

    private fun getTimetables() {
        list_lessons.clear()
        // запрос документов расписания
        db.collection("lessonstime").document("LxTrsAIg81E96zMSg0SL").get()
            .addOnSuccessListener { lesson_time -> // расписание звонков
                db.collection("lessons").orderBy("day", Query.Direction.ASCENDING).get()
                    .addOnSuccessListener { lesson_query -> // получаем всё расписание на все дни
                        repeat(lesson_query.size()) { // проходимся по каждому документу
                            val lesson = lesson_query.documents[it] // получаем текущий документ
                            var error = 0
                            repeat(
                                lesson.getLong("lessonsCount")!!
                                    .toInt()
                            ) { loop -> // проходимся по списку уроков в дне
                                val loopindex: Int = loop + 1 // получаем не 0-based индекс
                                var offset = lesson.getLong("timeOffset")!!
                                when (lesson.get("${loopindex}_parent")) {
                                    null -> {
                                        offset -= error
                                        list_lessons.add(
                                            Lesson( // добавляем урок в массив
                                                cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                                name = lesson.getString("${loopindex}_name")!!, // название предмета (пример: Математика)
                                                startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                                endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                                day = lesson.getLong("day")!! // номер дня в неделе, ПН=1, СБ=6
                                            )
                                        )
                                    }
                                    else -> {
                                        if (lesson.getBoolean("${loopindex}_parent")!!) {
                                            offset -= error
                                            list_lessons.add(
                                                Lesson( // добавляем урок в массив
                                                    cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                                    name = lesson.getString("${loopindex}_name")!!, // название предмета (пример Математика)
                                                    startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                                    endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                                    day = lesson.getLong("day")!!
                                                )
                                            ) // номер дня в неделе, ПН=1, СБ=6))
                                        } else {
                                            error++
                                            offset -= error
                                            list_lessons.add(
                                                Lesson( // добавляем урок в массив
                                                    cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                                    name = lesson.getString("${loopindex}_name")!!, // название предмета (пример Математика)
                                                    startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                                    endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                                    day = lesson.getLong("day")!! // номер дня в неделе, ПН=1, СБ=6
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        getNextLesson("Экономика")
                        getNextLesson("ОБЖ")
                        getNextLesson("Химия")
                    }
            }
    } // Получили расписание

    fun getNextLesson(lesson_name: String): Long {

        val cal = Calendar.getInstance(TimeZone.getDefault())
        val lessons = ArrayList<Lesson>()

        //val listLessonsPlus = ArrayList<Lesson>(list_lessons)
        //listLessonsPlus.forEach {
        //    it.day--
        //}

        // Добавляем предметы остатка недели
        list_lessons.forEach {
            if ((it.day) > cal.get(DAY_OF_WEEK)){
                val c = it
                val tmp = Lesson(c.cab, c.name, c.startAt, c.endAt, c.day)
                lessons.add(tmp)
            }
        }

        // Добавляем предметы следующей недели
        repeat(list_lessons.size) {
            val c = list_lessons[it]
            val tmp = Lesson(c.cab, c.name, c.startAt, c.endAt, c.day + 7)
            lessons.add(tmp)
        }

        lessons.forEach {
            it.day -= cal.get(DAY_OF_WEEK)-1
        }

        // ищем совпадающий по имени предмет в списке и возвращаем его
        lessons.forEach {
            if (lesson_name == it.name){
                Log.d("TEST", "selected: $it")
                Log.d("TEST", ((System.currentTimeMillis() / 1000) + ((it.day+1) * DAY_IN_SECONDS)).toString())
                return (System.currentTimeMillis() / 1000) + ((it.day+1) * DAY_IN_SECONDS)
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
                firebaseAuthWithGoogle(account.idToken!!)

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
                    val user = auth.currentUser!!
                    uid = auth.currentUser!!.uid

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

        fun checkPermission(r: String): Boolean {
            repeat(groups.size) { groupIndex ->
                repeat(groups[groupIndex].rights!!.size) { rightIndex ->
                    return groups[groupIndex].rights!![rightIndex].contains(r)
                }
            }
            return false
        }

        fun getAllPermissions(): ArrayList<String> {
            var permissions = ArrayList<String>()
            repeat(groups.size) { groupIndex ->
                repeat(groups[groupIndex].rights!!.size) { rightIndex ->
                    permissions.add(groups[groupIndex].rights!![rightIndex])
                }
            }
            return permissions
        }

    }

    private fun checkUserInDatabase(fire_user: FirebaseUser) {
        db.collection("users").document(fire_user.uid).get().addOnSuccessListener {
            if (it.exists()) {

                user = it.toObject<User>()!!

                getRights()

            } else {

                val data = hashMapOf<String, Any>(
                    "email" to fire_user.email!!
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
        db.collection("groups").whereArrayContains("users", uid!!).get().addOnSuccessListener {
            repeat(it.size()) { loopIndex ->
                val a = it.documents[loopIndex].toObject<Group>()!!.rights
                a.toString()
                user.groups.add(it.documents[loopIndex].toObject<Group>()!!)
                Log.w(F, user.groups.toString())
            }

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
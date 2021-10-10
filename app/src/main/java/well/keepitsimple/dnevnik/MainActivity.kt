package well.keepitsimple.dnevnik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.onesignal.OneSignal
import well.keepitsimple.dnevnik.ui.tasks.TaskItem
import well.keepitsimple.dnevnik.ui.tasks.TasksFragment
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import kotlin.collections.ArrayList

const val ONESIGNAL_APP_ID = "b5aa6c76-4619-4497-9b1e-2e7a1ef4095f"
const val DAYINSECONDS = 86400

class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    val F: String = "Firebase"
    private val RC_SIGN_IN: Int = 123

    var uid: String? = null
    var user: User = User()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val db = FirebaseFirestore.getInstance()
    val list_lessons = ArrayList<Lesson>()
    val tasks = ArrayList<TaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Logging set to help debug issues, remove before releasing your app.
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
                    }
            }
    }

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

    fun getNextLesson(lesson_name: String): Long {
        val week: ArrayList<Lesson> = ArrayList()
        val day = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.DAY_OF_WEEK) - 1
        if (day < 7) {
            repeat(list_lessons.size) {
                if (list_lessons[it].day > day) {
                    week.add(list_lessons[it])
                }
            }
        }
        repeat(list_lessons.size) { e ->
            week.add(list_lessons[e])
        }
        // Заполнили массивы
        var d: Long
        var index = 0
        d = if (week.size != 0) {
            week[0].day
        } else {
            0
        }
        repeat(week.size) {
            if (d != week[it].day) {
                d = week[it].day
                index++
            }
            d++
            if (week[it].name == lesson_name) {
                if (d == day.toLong()) {
                    return ((System.currentTimeMillis() / 1000) + (7 * DAYINSECONDS)) // если сегодня пн, а урок в пн (раз в неделю), то задаём на след. неделю в тот же день
                } else if (d < day) {
                    return ((System.currentTimeMillis() / 1000) + ((7 - day) + d) * DAYINSECONDS)
                } else {
                    return ((System.currentTimeMillis() / 1000) + d * DAYINSECONDS)
                }
            }
        }

        Log.d(
            "TIME",
            "Lesson time incorrect or lesson does not exist! lessons: $list_lessons \n ${System.currentTimeMillis() / 1000}"
        )
        return 0
    } // TODO:неправильный результат

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
        val groups: ArrayList<Group>? = ArrayList()
    ) {

        fun checkPermission(p: String){

        }

    }

    private fun checkUserInDatabase(fire_user: FirebaseUser) {
        db.collection("users").document(fire_user.uid).get().addOnSuccessListener {
            if (it.exists()) {

                user = it.toObject<User>()!!

                getRights()

                Log.w(F, "Ты есть в базе")

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
            repeat(it.size()){ loopIndex ->

                user.groups!!.add(it.documents[loopIndex].toObject<Group>()!!)

                //val doc_rights:Array<String> = it.documents[loopIndex]["rights"] as Array<String>
                //doc_rights.forEach { permission ->
                //    user_permissions.addUnique(permission)
                //}
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

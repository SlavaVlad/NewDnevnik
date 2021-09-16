package well.keepitsimple.dnevnik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
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
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.work.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import well.keepitsimple.dnevnik.ui.tasks.TaskItem
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import kotlin.math.ceil


class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    val F: String = "Firebase"
    private val RC_SIGN_IN: Int = 123

    var uid:String? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val db = FirebaseFirestore.getInstance()
    val list_lessons = ArrayList<Lesson>()
    val tasks = ArrayList<TaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun getDeadlineInDays(timestamp: Timestamp?): Double {
        return ceil(((timestamp!!.seconds.toDouble()) - System.currentTimeMillis() / 1000) / 86400)
    }

    private fun getTimetables() {
        list_lessons.clear()
        // запрос документов расписания
        db.collection("lessonstime").document("LxTrsAIg81E96zMSg0SL").get()
            .addOnSuccessListener { lesson_time -> // расписание звонков
                db.collection("lessons").get()
                    .addOnSuccessListener { lesson_query -> // получаем всё расписание на все дни
                        repeat(lesson_query.size()) { // проходимся по каждому документу
                            val lesson = lesson_query.documents[it] // получаем текущий документ
                            var error = 0
                            repeat(lesson.getLong("lessonsCount")!!
                                .toInt()) { loop -> // проходимся по списку уроков в дне
                                val loopindex: Int = loop + 1 // получаем не 0-based индекс
                                var offset = lesson.getLong("timeOffset")!!
                                when (lesson.get("${loopindex}_parent")) {
                                    null -> {
                                        offset -= error
                                        list_lessons.add(Lesson( // добавляем урок в массив
                                            cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                            name = lesson.getString("${loopindex}_name")!!, // название предмета (пример: Математика)
                                            startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                            endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                            day = lesson.getLong("day")!! // номер дня в неделе, ПН=1, СБ=6
                                        ))
                                    }
                                    else -> {
                                        if (lesson.getBoolean("${loopindex}_parent")!!) {
                                            offset -= error
                                            list_lessons.add(Lesson( // добавляем урок в массив
                                                cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                                name = lesson.getString("${loopindex}_name")!!, // название предмета (пример Математика)
                                                startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                                endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                                day = lesson.getLong("day")!!)) // номер дня в неделе, ПН=1, СБ=6))
                                        } else {
                                            error++
                                            offset -= error
                                            list_lessons.add(Lesson( // добавляем урок в массив
                                                cab = lesson.getLong("${loopindex}_cab")!!, // кабинет
                                                name = lesson.getString("${loopindex}_name")!!, // название предмета (пример Математика)
                                                startAt = lesson_time.getString("${loopindex + offset}_startAt")!!, // время начала урока 09:15, например
                                                endAt = lesson_time.getString("${loopindex + offset}_endAt")!!, // время конца урока
                                                day = lesson.getLong("day")!! // номер дня в неделе, ПН=1, СБ=6
                                            ))
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
    }

    // START LOGIN
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if ( currentUser == null) {
            signIn()
        } else {
            uid = currentUser.uid
            checkUserInDatabase(currentUser)
        }
    }

    fun alert(title: String, btn: String, message: String){
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setNeutralButton(btn){ dialog, id ->
            }
            .show()
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
                    val user = auth.currentUser
                    uid = user!!.uid

                        checkUserInDatabase(user)

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(F, "signInWithCredential:failure", task.exception)
                }
            }
    }
    private fun checkUserInDatabase(user: FirebaseUser) {

        db.collection("users").document(user.uid).addSnapshotListener { doc, error ->
            if (doc != null && doc.getBoolean("isStudent") != null && doc.getBoolean("isAdmin") != null) {

                Log.w(F, "Ты есть в базе")

            } else {

                val data = hashMapOf<String, Any>(
                    "isStudent" to true,
                    "isAdmin" to false,
                    "email" to user.email!!
                )

                val docRef = db.collection("users").document(user.uid)
                docRef.set(data).addOnSuccessListener {
                    Log.w(F, "User data write successfully")
                }.addOnFailureListener {
                    Log.w(F, "Error writing user data - ${it.message}")
                }

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
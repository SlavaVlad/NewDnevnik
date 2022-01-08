package well.keepitsimple.dnevnik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.onesignal.OneSignal
import io.github.tonnyl.whatsnew.WhatsNew
import io.github.tonnyl.whatsnew.item.WhatsNewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.tasks.Task
import well.keepitsimple.dnevnik.ui.timetables.Lesson
import java.util.*
import java.util.Calendar.DAY_OF_WEEK
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

const val ONESIGNAL_APP_ID = "b5aa6c76-4619-4497-9b1e-2e7a1ef4095f"
const val DAY_S = 86400
const val WEEK = 7

class MainActivity : AppCompatActivity(), CoroutineScope {

    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN: Int = 123
    val F: String = "Firebase"

    var uid: String? = null
    var user: User = User()
    var toEdit: DocumentSnapshot? = null

    var isTimetablesComplete = false

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val db = FirebaseFirestore.getInstance()
    var list_lessons = ArrayList<Lesson>()
    val tasks = ArrayList<Task>()
    val TAG = "MainActivity"
    var mAdView: AdView? = null

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onStart() {
        super.onStart()

        val wn = WhatsNew.newInstance(
            WhatsNewItem("Группы",
                "Теперь можно создавать классы и добавлять в них учеников!",
                R.drawable.ic_group),
            WhatsNewItem("Инвайты",
                "Вводите код, который вам дал учитель и вы без всяких затруднений становитесь учеником определённого класса",
                R.drawable.ic_link),
            WhatsNewItem("Скорость",
                "Запросов стало немного меньше и они вынесены в свои потоки, поэтому приложение будет меньше тормозить при открытии вкладки меню, например",
                R.drawable.ic_speed),
            WhatsNewItem("Как говаривал Chrome",
                "\"В этом обновлении мы повысили стабильность и производительность\"©",
                R.drawable.ic_citata),
        )
        wn.titleText = "Что нового?"
        wn.buttonText = "В приложение"
        //wn.buttonBackground = R.color.design_default_color_secondary
        //wn.buttonTextColor = R.color.white
        wn.presentAutomatically(this@MainActivity)

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            signIn()
        } else {
            checkUserInDatabase(currentUser)
            uid = currentUser.uid
        }

        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //MobileAds.initialize(this)
        mAdView = findViewById(R.id.adBanner_tasks)
        //val adRequest = AdRequest.Builder().build()
        //mAdView !!.loadAd(adRequest)
        mAdView !!.isVisible = false

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ca-app-pub-7054194174793904/9054046799 --

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
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tasks, R.id.nav_lk, R.id.nav_settings, R.id.nav_timetables, R.id.nav_groups
            ),
            drawerLayout
        )

    }

    private fun getTimetables() {
        // запрос документов расписания
        try {
            db.collection("groups")
                .document(user.getGroupByType("school").id !!)
                .collection("lessonstime")
                .get()
                .addOnSuccessListener { lesson_time_query -> // расписание звонков
                    db.collection("groups")
                        .document(user.getGroupByType("school").id !!)
                        .collection("groups")
                        .document(user.getGroupByType("class").id !!)
                        .collection("lessons")
                        .orderBy("day", Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { lesson_query -> // получаем всё расписание на все дни
                            val lesson_time = lesson_time_query.documents[0]
                            lesson_query.forEach { // проходимся по каждому документу
                                Log.d(TAG, it.getLong("day").toString())
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
                                                    groupId = lesson.getString("${loopindex + offset}_group")!!,
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
                                                        day = lesson.getLong("day") !!// номер дня в неделе, ПН=1, СБ=6
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // list_lessons.sortByDescending { list_lessons -> list_lessons.day }

                            Log.e(TAG, "getTimetables: ")

                            isTimetablesComplete = true

                            Log.d(TAG, list_lessons.toString())
                        }
                }
        } catch (e: NullPointerException) {
            throw Exception("No groups found")
        }
    }

    fun getNextLesson(lesson_name: String): Long {

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val today = calendar.get(DAY_OF_WEEK) - 1
        val now = (calendar.timeInMillis / 1000)
        val tmpLessons = ArrayList<Lesson>()

        list_lessons.forEach {
            if (it.day > today) {
                val tmp = Lesson(it.cab, it.name, it.startAt, it.endAt, it.day)
                tmpLessons.add(tmp)
            }
        }

        list_lessons.forEach {
            val tmp = Lesson(it.cab, it.name, it.startAt, it.endAt, it.day)
            tmpLessons.add(tmp)
        }

        Log.d(TAG, tmpLessons.toString())

        // ищем совпадающий по имени предмет в списке и возвращаем его
        var daysPassed = 1
        var loopindex = 0
        tmpLessons.forEach {
            if (loopindex > 0) {
                if (tmpLessons[loopindex - 1].day != it.day) {
                    daysPassed ++
                }
            }
            if (lesson_name == it.name) {

                Log.d(TAG, "Today: ${calendar.get(DAY_OF_WEEK) - 1}")
                Log.d(TAG, "Selected: $it")
                Log.d(TAG, "Returned: ${(now + ((it.day) * DAY_S))}")

                if (today < it.day) {
                    return (now + (daysPassed * DAY_S))
                } else {
                    return (now + ((daysPassed + 1) * DAY_S))
                }
            }
            loopindex ++
        }
        return 0
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
        Log.e(TAG, "t: $title m: $message")
        Log.e(TAG, source)
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
        var uid: String? = null,
        val groups: ArrayList<Group> = ArrayList(),
    ) {

        val db = FirebaseFirestore.getInstance()

        fun getGroupByType(type: String): Group {
            groups.forEach {
                if (it.type == type) {
                    return it
                }
            }
            return Group()
        }

        fun isAllowedInGroup(permission: String, group: Group): Boolean {
            if (group.rights !!.contains(permission)) {
                return true
            } else if (group.admins !!.contains(this.uid)) {
                if (group.admins !![this.uid] !!.contains(permission)) {
                    return true
                }
            }
            return false
        }

        fun isAllowedAsAdmin(permission: String, group: Group): Boolean {
            return group.admins !![this.uid] !!.contains(permission)
        }

        fun getGroupsWhereAdmin(): List<Group> {
            val toReturn = mutableListOf<Group>()

            groups.forEach {
                if (it.admins !!.contains(uid)) {
                    toReturn.add(it)
                }
            }

            return toReturn
        }

        @Deprecated("Deprecated in class Groups system", ReplaceWith("getPermissionsByGroup()"))
        fun isAllow(p: String): Boolean {
            return this.getAllPermissions().contains(p)
        }

        @Deprecated("Deprecated in class Groups system", ReplaceWith("getPermissionsByGroup()"))
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
        db.collection("users").document(fire_user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {

                user = doc.toObject<User>() !!

                user.uid = doc.id

                getRights()
            } else {

                val data = hashMapOf<String, Any>(
                    resources.getString(R.string.familia) to "",
                    resources.getString(R.string.name) to "",
                    resources.getString(R.string.otchestvo) to "",
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

        if (intent.getStringExtra("addGroup") != "") {
            db.collectionGroup("groups")
                .whereEqualTo("docId", intent.getStringExtra("addGroup"))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.documents.forEach { doc ->
                            (doc.get("users") as HashMap<String, Any?>)[uid!!] = null

                            when (doc["type"].toString()){
                                "class" -> doc.reference.parent.parent!!.get().addOnSuccessListener {
                                    (it.get("users") as HashMap<String, Any?>)[uid!!] = null
                                    it.reference.update(it.data!!)
                                }
                            }

                            doc.reference.update(doc.data!!)
                    }
                }
        }

        db.collectionGroup("groups")
            .whereEqualTo(FieldPath.of("users", "$uid"), null)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var index = 0
                querySnapshot.documents.forEach { // записываем группы в пользователя
                    user.groups.add(it.toObject<Group>() !!)
                    user.groups[index].doc = it
                    user.groups[index].id = it.id
                    it.id
                    Log.w(TAG, user.groups.toString())
                    index ++
                }
                if (user.groups.isEmpty()){
                    qr()
                }
                if (list_lessons.isEmpty()) {
                    getTimetables()
                }
            }

        db.collectionGroup("groups")
            .whereArrayContains("admins", uid !!)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var index = 0
                querySnapshot.documents.forEach {
                    user.groups.addUnique(it.toObject<Group>() !!)
                    user.groups[index].id = it.id
                    it.id
                    Log.w(TAG, user.groups.toString())
                    index ++
                }
            }

    }

    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            acceptInvite(result.contents)
        }
    }

    // Launch
    private fun qr() {
        barcodeLauncher.launch(ScanOptions().setOrientationLocked(true).setPrompt("Код, который дал вам учитель"))
    }

    private fun acceptInvite(code: String) {
        FirebaseFirestore
            .getInstance()
            .collectionGroup("groups")
            .whereEqualTo("docId", code)
            .limit(1)
            .get()
            .addOnSuccessListener { q ->
                val doc = q.documents[0]
                val data = hashMapOf<String, Any>(
                    "users" to hashMapOf<String, Any?>(
                        uid!! to null,
                    )
                )
                doc.reference.update(data)
                Snackbar.make(binding.root.rootView, "Вы вступили в группу ${doc["name"]}", Snackbar.LENGTH_LONG).show()
            }
    }

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
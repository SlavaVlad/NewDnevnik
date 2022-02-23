package well.keepitsimple.dnevnik

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.instabug.library.Instabug
import com.instabug.library.invocation.InstabugInvocationEvent
import com.onesignal.OneSignal
import io.github.tonnyl.whatsnew.WhatsNew
import io.github.tonnyl.whatsnew.item.WhatsNewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.greenrobot.eventbus.EventBus
import well.keepitsimple.dnevnik.databinding.ActivityMainBinding
import well.keepitsimple.dnevnik.login.Group
import well.keepitsimple.dnevnik.ui.groups.User
import well.keepitsimple.dnevnik.ui.groups.UserDataSetEvent
import well.keepitsimple.dnevnik.ui.tasks.Task
import well.keepitsimple.dnevnik.ui.timetables.objects.Lesson
import well.keepitsimple.dnevnik.ui.timetables.objects.LessonsTime
import well.keepitsimple.dnevnik.ui.timetables.objects.Timetable
import java.util.*
import java.util.Calendar.DAY_OF_WEEK
import kotlin.coroutines.CoroutineContext

const val ONESIGNAL_APP_ID = "b5aa6c76-4619-4497-9b1e-2e7a1ef4095f"
const val DAY_S = 86400
const val WEEK = 7

class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN: Int = 123
    val F: String = "Firebase"

    var uid: String? = null
    var user: User = User()
    var toEdit: DocumentSnapshot? = null

    var isTimetablesComplete = false

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val db = FirebaseFirestore.getInstance()
    var timetable: Timetable? = null
    val tasks = ArrayList<Task>()
    var docTime = mutableListOf<LessonsTime>()
    val TAG = "MainActivity"
    var mAdView: AdView? = null
    private val remoteConfig = Firebase.remoteConfig

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
            WhatsNewItem(
                "Приглашения",
                "Приглашение учеников работает теперь только через ссылки-приглашения",
                R.drawable.ic_link
            ),
            WhatsNewItem(
                "Группы",
                "Каждая группа должна иметь свой уникальный тег (имя)",
                R.drawable.ic_tag
            ),
            WhatsNewItem(
                "Расписание",
                "Расписание полностью переработано",
                R.drawable.ic_timetables
            ),
            WhatsNewItem(
                "Повышена стабильность",
                "Переработан редактор создания группы",
                R.drawable.ic_edit
            ),
        )

        wn.titleText = "Что нового?"
        wn.buttonText = "Хорошо"
        //wn.buttonBackground = R.color.design_default_color_secondary
        //wn.buttonTextColor = R.color.white
        wn.presentAutomatically(this@MainActivity)

        checkDeeplink()

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
        mAdView!!.isVisible = false

    }

    private fun checkDeeplink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                val link: Uri? = pendingDynamicLinkData?.link
                if (link?.getQueryParameter("invite") != null) {
                    val bottomActionDialog = BottomSheetDialog(
                        this, R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog
                    )
                    val bottomView = LayoutInflater.from(this)
                        .inflate(
                            R.layout.mds_main_invite,
                            findViewById(R.id.lay_invite)
                        )
                    bottomView.findViewById<Button>(R.id.btn_acceptInvite).setOnClickListener {
                        acceptInvite(
                            link.getQueryParameter("invite").toString(),
                            object : OnCompletedListener {
                                override fun onCompleted(success: Boolean, msg: String) {
                                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
                                        .show()
                                    bottomActionDialog.dismiss()
                                }
                            })
                    }
                    bottomView.findViewById<TextView>(R.id.tv_group_name).text =
                        link.getQueryParameter("name").toString()
                    bottomActionDialog.setContentView(bottomView)
                    bottomActionDialog.show()
                }

            }
            .addOnFailureListener(this) { e -> Log.e(TAG, "getDynamicLink:onFailure", e) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 30
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    alert(
                        "Ошибка конфигурации",
                        "Не удалось получить конфигурацию приложения",
                        "OnCreate()"
                    )
                }
            }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Instabug.Builder(application, "9afe4d789c62e398a755bc2b0a3eb223").setInvocationEvents(
            InstabugInvocationEvent.SCREENSHOT,
            InstabugInvocationEvent.TWO_FINGER_SWIPE_LEFT
        ).build()

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
                R.id.nav_tasks,
                R.id.nav_lk,
                R.id.nav_settings,
                R.id.nav_timetables,
                R.id.nav_groups
            ),
            drawerLayout
        )
    }

    private fun acceptInvite(id: String, onCompleted: OnCompletedListener) {
        val db = FirebaseFirestore.getInstance()
        db.collection("invites")
            .document(id)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    db.collectionGroup("groups")
                        .whereEqualTo("id", (it["inviteTo"] as String))
                        .limit(1L)
                        .get()
                        .addOnSuccessListener { q ->
                            val data = hashMapOf<String, Any?>(
                                "users" to hashMapOf<String, Any?>(
                                    user.uid to null
                                )
                            )
                            q.documents[0].reference.set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    launch { reloadGroups() }.invokeOnCompletion {
                                        onCompleted.onCompleted(true, "Успешно")
                                    }
                                }.addOnFailureListener {
                                    onCompleted.onCompleted(false, "Не удалось записать")
                                }
                        }.addOnFailureListener {
                            onCompleted.onCompleted(false, "Группа не найдена")
                        }
                } else {
                    onCompleted.onCompleted(false, "Документа не существует!")
                }
            }.addOnFailureListener {
                onCompleted.onCompleted(false, "Приглашение не найдено")
            }
    }

    private fun getTimetables() {
        // запрос документов расписания
        timetable?.lessons?.clear()
        val list_lessons = ArrayList<Lesson>()
        db.collection("groups")
            .document(user.getGroupByType("school").id!!)
            .collection("lessonstime")
            .get()
            .addOnSuccessListener { timeTmp -> // расписание звонков
                val _docTime = (timeTmp.documents[0]["time"] as List<HashMap<String, String>>)
                _docTime.forEach {
                    docTime.add(LessonsTime((it["startAt"] as String), (it["endAt"] as String)))
                }
                db.collection("groups")
                    .document(user.getGroupByType("school").id!!) // Получаем уроки за определённый день
                    .collection("groups")
                    .document(user.getGroupByType("class").id!!)
                    .collection("timetables")
                    .get()
                    .addOnSuccessListener { q ->
                        q.documents.forEach { doc ->
                            (doc["lessons"] as List<HashMap<String, Any>>).forEach { l ->
                                val lesson = Lesson(
                                    (l["index"] as Long).toInt(),
                                    (l["cab"] as String),
                                    (l["name"] as String),
                                    (l["teacher"] as String?),
                                    docTime[(l["index"] as Long).toInt()],
                                    doc.getLong("dow")!!.toInt(),
                                )
                                lesson.groupId = (l["groupId"] as String?)
                                list_lessons.add(lesson)
                            }
                        }
                        timetable = Timetable(list_lessons)
                    }
            }

        isTimetablesComplete = true

        Log.e(TAG, list_lessons.toString())

    }

    fun getNextLesson(lesson_name: String): Long {

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val today = calendar.get(DAY_OF_WEEK) - 1
        val now = (calendar.timeInMillis / 1000)
        val tmpLessons = ArrayList<Lesson>()

        timetable?.lessons?.forEach {
            if (it.day > today) {
                val tmp = it.copy()
                tmpLessons.add(tmp)
            }
        }

        timetable?.lessons?.forEach {
            val tmp = it.copy()
            tmpLessons.add(tmp)
        }

        Log.d(TAG, tmpLessons.toString())

        // ищем совпадающий по имени предмет в списке и возвращаем его
        var daysPassed = 1
        var loopindex = 0
        tmpLessons.forEach {
            if (loopindex > 0) {
                if (tmpLessons[loopindex - 1].day != it.day) {
                    daysPassed++
                }
            }
            if (lesson_name == it.name) {

                return if (today < it.day) {
                    (now + (daysPassed * DAY_S))
                } else {
                    (now + ((daysPassed + 1) * DAY_S))
                }
            }
            loopindex++
        }
        return 0
    } // DO NOT TOUCH IT!!!

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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.e(F, "Google sign in failed", e)
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
                        "Дальнейшая работа невозможна: ${task.exception}",
                        "firebaseAuthWithGoogle"
                    )
                }
            }
    }

    private fun checkUserInDatabase(fire_user: FirebaseUser) {
        db.collection("users").document(fire_user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                user.setUserID(doc.id)
                launch { reloadGroups() }
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

    private suspend fun reloadGroups() {
        db.collectionGroup("groups")
            .whereEqualTo(FieldPath.of("users", uid), null)
            .get()
            .addOnSuccessListener { query ->
                val groups = mutableListOf<Group>()
                query.documents.forEach {
                    try {
                        val group = it.toObject<Group>()!!
                        group.dRef = it.reference
                        groups.add(group)
                    } catch (e: NullPointerException) {
                        Log.e("User", "loadGroups() group can't be deserialized: $e")
                    }
                }
                user.groupsUser = groups
            }.addOnFailureListener {
                Log.e("F", "reloadGroups: $it")
            }.await()
        db.collectionGroup("groups")
            .whereArrayContains("adminsMembers", user.uid)
            .get()
            .addOnSuccessListener { query ->
                val groups = mutableListOf<Group>()
                query.documents.forEach {
                    try {
                        val group = it.toObject<Group>()!!
                        group.dRef = it.reference
                        groups.add(group)
                    } catch (e: NullPointerException) {
                        Log.e("User", "loadGroups() group can't be deserialized: $e")
                    }
                }
                user.groupsAdmin = groups
                if (user.getGroupByType("school").id == null || user.getGroupByType("class").id == null) {
                    //...
                } else {
                    getTimetables()
                    EventBus.getDefault()
                        .post(UserDataSetEvent(user.uid, user.groupsAdmin, user.groupsUser))
                }
            }.addOnFailureListener {
                Log.e("F", "reloadGroups: $it")
            }.await()

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

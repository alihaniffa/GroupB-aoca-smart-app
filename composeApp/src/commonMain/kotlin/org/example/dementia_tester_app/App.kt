package org.example.dementia_tester_app

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.example.dementia_tester_app.auth.AuthResult
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserProfileService
import org.example.dementia_tester_app.data.UserSettingsService
import org.example.dementia_tester_app.data.UserType
import org.example.dementia_tester_app.notifications.LocalNotificationManagerAdapter
import org.example.dementia_tester_app.notifications.NotificationManagerProvider
import org.example.dementia_tester_app.notifications.ReminderChannels
import org.example.dementia_tester_app.notifications.ReminderHelper
import org.example.dementia_tester_app.notifications.ReminderIds
import org.example.dementia_tester_app.notifications.ReminderPolicy
import org.example.dementia_tester_app.ui.components.AppMenuContent
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.components.PageLayout
import org.example.dementia_tester_app.ui.screens.Activities
import org.example.dementia_tester_app.ui.screens.AppointmentHistory
import org.example.dementia_tester_app.ui.screens.BookAppointment
import org.example.dementia_tester_app.ui.screens.Chat
import org.example.dementia_tester_app.ui.screens.Contact
import org.example.dementia_tester_app.ui.screens.HealthSurvey
import org.example.dementia_tester_app.ui.screens.Help
import org.example.dementia_tester_app.ui.screens.Profile
import org.example.dementia_tester_app.ui.screens.Settings
import org.example.dementia_tester_app.ui.screens.caregiver.CaregiverDashboard
import org.example.dementia_tester_app.ui.screens.dashboard.Dashboard
import org.example.dementia_tester_app.ui.screens.doctor.DoctorDashboard
import org.example.dementia_tester_app.ui.screens.login.EmailVerification
import org.example.dementia_tester_app.ui.screens.login.ForgotPassword
import org.example.dementia_tester_app.ui.screens.login.Login
import org.example.dementia_tester_app.ui.screens.login.SignUp

@Composable
fun App() {

    val settingsService =
        remember {
            UserSettingsService()
        }

    var userSettings by remember {
        mutableStateOf(
            org.example.dementia_tester_app.data.UserSettings()
        )
    }

    var hasLoadedSettings by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        settingsService
            .loadSettings { result ->

                if (
                    result is DatabaseResult.Success
                ) {
                    userSettings =
                        result.data
                }

                hasLoadedSettings =
                    true
            }
    }

    org.example.dementia_tester_app.ui.theme.DementiaTesterTheme(
        darkTheme =
            userSettings.darkMode
    ) {

        val drawerState =
            rememberDrawerState(
                initialValue =
                    DrawerValue.Closed
            )

        val scope =
            rememberCoroutineScope()

        val authService =
            remember {
                AuthService()
            }

        val userProfileService =
            remember {
                UserProfileService()
            }

        /*
         * State to track the signed-in user's role.
         */
        var userType by remember {
            mutableStateOf(
                UserType.USER
            )
        }

        /**
         * Return the correct dashboard
         * for the currently signed-in role.
         */
        val getDashboardType = {

            when (
                userType
            ) {

                UserType.DOCTOR ->
                    "DoctorDashboard"

                UserType.CAREGIVER ->
                    "CaregiverDashboard"

                UserType.USER ->
                    "Dashboard"
            }
        }

        /*
         * State to track whether we are
         * loading the current user profile.
         */
        var isLoadingProfile by remember {
            mutableStateOf(false)
        }

        /*
         * Email used during verification.
         */
        var userEmail by remember {
            mutableStateOf("")
        }

        var profileRefreshKey by remember {
            mutableStateOf(0)
        }

        var currentScreen by remember {

            mutableStateOf(

                if (
                    authService.isUserSignedIn()
                ) {

                    /*
                     * If already signed in,
                     * load profile to determine role.
                     */
                    isLoadingProfile =
                        true

                    "Loading"

                } else {

                    "Login"
                }
            )
        }

        /*
         * Schedule Health Survey reminder
         * every Sunday at 6 PM local time.
         */
        LaunchedEffect(Unit) {

            val mgr =
                NotificationManagerProvider
                    .getNotificationManager()

            val helper =
                ReminderHelper(
                    LocalNotificationManagerAdapter(
                        mgr
                    )
                )

            @RequiresApi(
                Build.VERSION_CODES.O
            )
            fun nextSunday6pmUtcMillis(): Long {

                val tz =
                    TimeZone
                        .currentSystemDefault()

                val nowInstant =
                    Clock.System.now()

                val nowLocal =
                    nowInstant
                        .toLocalDateTime(
                            tz
                        )

                val todayDow =
                    nowLocal
                        .date
                        .dayOfWeek

                val isBeforeOrAt6pm =
                    nowLocal.hour < 18 ||
                            (
                                    nowLocal.hour == 18 &&
                                            nowLocal.minute == 0 &&
                                            nowLocal.second == 0 &&
                                            nowLocal.nanosecond == 0
                                    )

                val daysUntil =
                    when (
                        todayDow
                    ) {

                        DayOfWeek.MONDAY ->
                            6

                        DayOfWeek.TUESDAY ->
                            5

                        DayOfWeek.WEDNESDAY ->
                            4

                        DayOfWeek.THURSDAY ->
                            3

                        DayOfWeek.FRIDAY ->
                            2

                        DayOfWeek.SATURDAY ->
                            1

                        DayOfWeek.SUNDAY ->
                            if (
                                isBeforeOrAt6pm
                            ) {
                                0
                            } else {
                                7
                            }

                        else ->
                            0
                    }

                val targetDate =
                    kotlinx.datetime.LocalDate
                        .fromEpochDays(
                            nowLocal
                                .date
                                .toEpochDays() +
                                    daysUntil
                        )

                val targetLocal =
                    LocalDateTime(
                        targetDate,
                        LocalTime(
                            hour = 18,
                            minute = 0
                        )
                    )

                return targetLocal
                    .toInstant(
                        tz
                    )
                    .toEpochMilliseconds()
            }

            val utcAt =
                nextSunday6pmUtcMillis()

            helper.upsertAt(
                id =
                    ReminderIds.healthSurvey(
                        "weekly"
                    ),
                message =
                    "It's time for your weekly health survey.",
                utcMillis =
                    utcAt,
                policy =
                    ReminderPolicy(
                        channel =
                            ReminderChannels.HEALTH,
                        allowAfterReboot =
                            true
                    )
            )

            println(
                "Weekly health survey scheduled for $utcAt"
            )
        }

        /*
         * Load user profile after login
         * so the app knows which dashboard
         * should be displayed.
         */
        LaunchedEffect(
            authService.isUserSignedIn(),
            isLoadingProfile
        ) {

            if (
                authService.isUserSignedIn() &&
                isLoadingProfile
            ) {

                userProfileService
                    .getCurrentUserProfile { result ->

                        when (
                            result
                        ) {

                            is DatabaseResult.Success -> {

                                userEmail =
                                    result.data.email

                                userType =
                                    result.data.userType

                                /*
                                 * Once the profile role is known,
                                 * route to the correct dashboard.
                                 */
                                if (
                                    authService.isEmailVerified()
                                ) {

                                    currentScreen =
                                        getDashboardType()

                                } else {

                                    currentScreen =
                                        "EmailVerification"
                                }
                            }

                            is DatabaseResult.Error -> {

                                if (
                                    result.message.contains(
                                        "Profile not found",
                                        ignoreCase =
                                            true
                                    )
                                ) {

                                    currentScreen =
                                        "Profile"

                                } else {

                                    authService
                                        .signOut()

                                    currentScreen =
                                        "Login"
                                }
                            }
                        }

                        isLoadingProfile =
                            false
                    }
            }
        }

        if (
            currentScreen == "Login" ||
            currentScreen == "SignUp" ||
            currentScreen == "ForgotPassword" ||
            currentScreen == "Loading" ||
            currentScreen == "EmailVerification"
        ) {

            when (
                currentScreen
            ) {

                "Login" -> {

                    Login(
                        onLogin = { email ->

                            userEmail =
                                email

                            if (
                                !authService.isEmailVerified()
                            ) {

                                authService
                                    .reloadUser { result ->

                                        when (
                                            result
                                        ) {

                                            is AuthResult.Success -> {

                                                if (
                                                    authService.isEmailVerified()
                                                ) {

                                                    isLoadingProfile =
                                                        true

                                                    currentScreen =
                                                        "Loading"

                                                } else {

                                                    currentScreen =
                                                        "EmailVerification"
                                                }
                                            }

                                            is AuthResult.Error -> {

                                                currentScreen =
                                                    "EmailVerification"
                                            }
                                        }
                                    }

                            } else {

                                isLoadingProfile =
                                    true

                                currentScreen =
                                    "Loading"
                            }
                        },

                        onSignUp = {

                            currentScreen =
                                "SignUp"
                        },

                        onForgotPassword = {

                            currentScreen =
                                "ForgotPassword"
                        }
                    )
                }

                "SignUp" -> {

                    SignUp(
                        onBack = {

                            currentScreen =
                                "Login"
                        },

                        onSignUpSuccess = { email ->

                            userEmail =
                                email

                            authService
                                .sendEmailVerification {

                                    currentScreen =
                                        "EmailVerification"
                                }
                        }
                    )
                }

                "EmailVerification" -> {

                    EmailVerification(
                        email =
                            userEmail,

                        onVerified = {

                            isLoadingProfile =
                                true

                            currentScreen =
                                "Loading"
                        },

                        onBack = {

                            currentScreen =
                                "Login"
                        }
                    )
                }

                "ForgotPassword" -> {

                    ForgotPassword(
                        onBack = {

                            currentScreen =
                                "Login"
                        }
                    )
                }

                "Loading" -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        LoadingSpinner()
                    }
                }
            }

        } else {

            /*
             * All authenticated app screens
             * use the navigation drawer.
             */
            ModalNavigationDrawer(
                drawerState =
                    drawerState,

                drawerContent = {

                    AppMenuContent(
                        onMenuItemClick = { menuItem ->

                            /*
                             * Dashboard now respects all roles:
                             *
                             * USER -> Dashboard
                             * DOCTOR -> DoctorDashboard
                             * CAREGIVER -> CaregiverDashboard
                             */
                            currentScreen =
                                if (
                                    menuItem == "Dashboard"
                                ) {

                                    getDashboardType()

                                } else {

                                    menuItem
                                }

                            scope.launch {

                                drawerState
                                    .close()
                            }
                        },

                        refreshKey =
                            profileRefreshKey
                    )
                }
            ) {

                PageLayout(
                    drawerState =
                        drawerState,

                    title =
                        when (
                            currentScreen
                        ) {

                            "DoctorDashboard",
                            "CaregiverDashboard" ->
                                "Dashboard"

                            else ->
                                currentScreen
                        }
                ) {

                    /*
                     * Render the correct screen.
                     */
                    when (
                        currentScreen
                    ) {

                        "Dashboard" -> {

                            Dashboard()
                        }

                        "DoctorDashboard" -> {

                            DoctorDashboard()
                        }

                        "CaregiverDashboard" -> {

                            CaregiverDashboard()
                        }

                        "Health Survey" -> {

                            HealthSurvey(
                                onBackToDashboard = {

                                    currentScreen =
                                        getDashboardType()
                                }
                            )
                        }

                        "Activities" -> {

                            Activities()
                        }

                        "Book Appointment" -> {

                            BookAppointment(
                                onCancel = {

                                    currentScreen =
                                        getDashboardType()
                                },

                                onSuccess = {

                                    currentScreen =
                                        "Appointment History"
                                }
                            )
                        }

                        "Appointment History" -> {

                            AppointmentHistory()
                        }

                        "Contact" -> {

                            Contact()
                        }

                        "Chat" -> {

                            Chat()
                        }

                        "Settings" -> {

                            Settings(
                                onAccountDeleted = {

                                    authService
                                        .signOut()

                                    userType =
                                        UserType.USER

                                    currentScreen =
                                        "Login"
                                },

                                onSettingsChanged = { updated ->

                                    userSettings =
                                        updated
                                }
                            )
                        }

                        "Help" -> {

                            Help()
                        }

                        "Profile" -> {

                            Profile(
                                onBack = {

                                    profileRefreshKey++

                                    isLoadingProfile =
                                        true

                                    currentScreen =
                                        "Loading"
                                }
                            )
                        }

                        "logout" -> {

                            authService
                                .signOut()

                            userType =
                                UserType.USER

                            currentScreen =
                                "Login"
                        }

                        else -> {

                            currentScreen =
                                if (
                                    authService.isUserSignedIn()
                                ) {

                                    getDashboardType()

                                } else {

                                    "Login"
                                }
                        }
                    }
                }
            }
        }
    }
}
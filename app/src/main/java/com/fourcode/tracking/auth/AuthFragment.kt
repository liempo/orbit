package com.fourcode.tracking.auth

import android.view.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import android.content.Context.MODE_PRIVATE
import androidx.navigation.fragment.findNavController

import com.google.android.material.snackbar.Snackbar

import okhttp3.*
import com.beust.klaxon.*
import timber.log.Timber

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.auth_fragment.*

import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import kotlin.random.Random


class AuthFragment : Fragment(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.auth_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Randomize splash screen
        video_layout.setPathOrUrl(
            getString(R.string.url_splash_video_format,
                Random.nextInt(1, 5))
        )

        // Initialize shared preferences object, for credentials
        val sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.shared_pref_credentials), MODE_PRIVATE)

        // Check if logged in first
        val loggedInId = sharedPreferences.getString(
           getString(R.string.shared_pref_id), null)

        // Check user type (standard or admin)
        val isLoggedInUserAdmin = (sharedPreferences.getString(
            getString(R.string.shared_pref_admin_id), null)  == null)

        if (loggedInId.isNullOrBlank().not() )
             findNavController().navigate(
                 if (isLoggedInUserAdmin)
                     AuthFragmentDirections.startStandard()
                else AuthFragmentDirections.startAdmin()
             )

        // Will make things faster if i don't initialize
        // the login_button's listener if above statement is true
        else login_button.setOnClickListener {
            val username = username_or_email_input.text.toString()
            val password = password_input.text.toString()

            // Check first if fields are empty
            if (username.isBlank() || password.isBlank())
                Snackbar.make(view,
                    R.string.error_missing_fields,
                    Snackbar.LENGTH_SHORT).show()

            if (username.isNotBlank() && password.isNotBlank()) launch {
                // Disable UI components while call
                username_or_email_input.isEnabled = false
                password_input.isEnabled = false
                login_button.isEnabled = false

                // Check if is an admin (standard user does not login with email)
                val isAdmin = isEmailValid(username)
                if (isAdmin) Snackbar.make(view,
                    R.string.msg_logging_in_as_admin,
                    Snackbar.LENGTH_LONG)
                    .show()

                // Run call and get response with context
                val response =
                    if (isAdmin) startAdminLogin(username, password)
                    else startStandardLogin(username, password)

                if (response.id.isNotEmpty()) {
                    Timber.d("Logged in with id: ${response.id}")

                    // Save credentials locally (this means user is logged in)
                    sharedPreferences.edit {
                        putString(getString(R.string
                            .shared_pref_token), response.token)
                        putString(getString(R.string
                            .shared_pref_id), response.id)

                        // if user is standard, adminId must be presenst
                        if (isAdmin.not()) putString(
                            getString(R.string.shared_pref_admin_id),
                            (response as StandardLoginResponse).adminId)
                    }

                } else
                // Show error if error is not empty
                    Snackbar.make(view,
                        response.error,
                        Snackbar.LENGTH_SHORT)
                        .show()

                // Enable UI componente
                username_or_email_input.isEnabled = true
                password_input.isEnabled = true
                login_button.isEnabled = true
            }
        }

    }

    private suspend fun isEmailValid(email: String): Boolean {
        // Fucking hell, this is an over kill
        val regex = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:" +
                "\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"" +
                "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f" +
                "\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-" +
                "\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:" +
                "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+" +
                "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:" +
                "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)" +
                "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|" +
                "[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c" +
                "\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[" +
                "\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

        return withContext(Dispatchers.Main) {
            email.matches(regex)
        }
    }

    private suspend fun startStandardLogin(
        username: String, password: String): StandardLoginResponse {
        // Create an okhttp3
        val client = OkHttpClient()

        // Create request body
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        // Create OkHttpRequest
        val request = Request.Builder()
            .url(BuildConfig.AuthApiUrl + "login")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            // Execute request and get response
            val response = client.newCall(request).execute()

            // Parse response
            return@withContext Klaxon().parse<StandardLoginResponse>(
                response.body!!.string())!!
        }
    }

    private suspend fun startAdminLogin(
        email: String, password: String): AdminLoginResponse {
        // Create an okhttp3
        val client = OkHttpClient()

        // Create request body
        val body = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        // Create OkHttpRequest
        val request = Request.Builder()
            .url(BuildConfig.AuthApiUrl + "login/admin")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            // Execute request and get response
            val response = client.newCall(request).execute()

            // Parse response
            return@withContext Klaxon().parse<AdminLoginResponse>(
                response.body!!.string())!!
        }
    }

    override fun onDestroy() {
        // Cancel all running co-routines
        job.cancel(); super.onDestroy()
    }

    interface LoginResponse {
        @Json(name = "token") val token: String
        val id: String
        val error: String
    }

    private data class StandardLoginResponse(
        override val token: String,
        @Json(name = "admin_id") val adminId: String = "",
        @Json(name = "user_id") override val id: String = "",
        override val error: String = ""
    ) : LoginResponse

    private data class AdminLoginResponse(
        override val token: String,
        @Json(name = "id") override val id: String = "",
        override val error: String = ""
    ) : LoginResponse

}

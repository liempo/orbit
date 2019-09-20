package com.fourcode.tracking.auth

import android.view.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import android.content.Context.MODE_PRIVATE

import com.google.android.material.snackbar.Snackbar

import okhttp3.*
import com.beust.klaxon.*
import timber.log.Timber

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.auth_fragment.*

import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R


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

        // Initialize shared preferences object, for credentials
        val sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.shared_pref_credentials), MODE_PRIVATE)

        // Check if logged in first
        val loggedInId = sharedPreferences.getString(
            getString(R.string.shared_pref_admin_id), null)

//        if (loggedInId.isNullOrBlank().not())
//            // TODO

        login_button.setOnClickListener {
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

                // Run call and get response with context
                val response =  login(username, password)

                // Log adminId
                Timber.d("adminId: ${response.adminId}")

                if (response.adminId.isNotEmpty()) {
                    // Save credentials locally (this means user is logged in)
                    sharedPreferences.edit {
                        putString(getString(R.string
                            .shared_pref_token), response.token)
                        putString(getString(R.string
                            .shared_pref_admin_id), response.adminId)
                        putString(getString(R.string
                            .shared_pref_user_id), response.userId)
                    }


                } else
                // Show error if error is not empty
                    Snackbar.make(view, response.error, Snackbar.LENGTH_SHORT).show()

                // Enable UI componente
                username_or_email_input.isEnabled = true
                password_input.isEnabled = true
                login_button.isEnabled = true
            }
        }

    }

    private suspend fun login(username: String, password: String): LoginResponse {
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
            return@withContext Klaxon().parse<LoginResponse>(
                response.body!!.string())!!
        }
    }

    override fun onDestroy() {
        // Cancel all running coroutines
        job.cancel(); super.onDestroy()
    }

    private data class LoginResponse(
        @Json(name = "token") val token: String = "",
        @Json(name = "user_id") val userId: String = "",
        @Json(name = "admin_id") val adminId: String = "",
        val error: String = ""
    )

}

package com.fourcode.tracking.auth

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.auth_fragment.*
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


class AuthFragment : Fragment(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val args: AuthFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.auth_fragment,
        container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize shared preferences object, for credentials
        val sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.shared_pref_credentials), MODE_PRIVATE
            )

        // Logout if toLogout is true
        if (args.toLogout) sharedPreferences.edit {
            remove(getString(R.string.shared_pref_id))
            remove(getString(R.string.shared_pref_token))
            remove(getString(R.string.shared_pref_admin_id))
        }

        // Check if logged in first
        val loggedInId = sharedPreferences.getString(
            getString(R.string.shared_pref_id), null
        )

        // Check user type (standard or admin)
        val isLoggedInUserAdmin = (sharedPreferences.getString(
            getString(R.string.shared_pref_admin_id), null
        ) == null)

        if (loggedInId.isNullOrBlank().not())
            findNavController().navigate(
                if (isLoggedInUserAdmin)
                    AuthFragmentDirections.startAdmin()
                else AuthFragmentDirections.startStandard()
            )

        login_button.setOnClickListener {
            val username = username_or_email_input.text.toString()
            val password = password_input.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) launch {
                // Disable UI components while call
                username_or_email_input.isEnabled = false
                password_input.isEnabled = false
                login_button.isEnabled = false

                val response = startStandardLogin(username, password)

                if (response.id.isNotEmpty()) {
                    Timber.d("Logged in with id: ${response.id}")

                    // Save credentials locally (this means user is logged in)
                    sharedPreferences.edit {
                        putString(
                            getString(
                                R.string
                                    .shared_pref_token
                            ), response.token
                        )
                        putString(
                            getString(
                                R.string
                                    .shared_pref_id
                            ), response.id
                        )

                        putString(
                            getString(R.string.shared_pref_admin_id),
                            response.adminId
                        )
                    }

                    findNavController().navigate(AuthFragmentDirections.startStandard())

                } else
                // Show error if error is not empty
                    Snackbar.make(
                        view,
                        response.error,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()

                // Enable UI componente
                username_or_email_input.isEnabled = true
                password_input.isEnabled = true
                login_button.isEnabled = true

                // Log error
                Timber.w("Orbit API Error Message: ${response.error}")
            } else {
                Snackbar.make(
                    view,
                    R.string.error_missing_fields,
                    Snackbar.LENGTH_SHORT
                ).show()

                Timber.w("Missing fields (username or password)")
            }
        }

    }

    private suspend fun startStandardLogin(
        username: String, password: String
    ): StandardLoginResponse {
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
            val response = client.newCall(request).execute().body!!

            // Parse result before closing response
            val result = Klaxon().parse<StandardLoginResponse>(response.string())!!

            // Close response to avoid leaks
            response.close()

            return@withContext result
        }
    }

    override fun onDestroy() {
        // Cancel all running co-routines
        job.cancel(); super.onDestroy()
    }

    private data class StandardLoginResponse(
        @Json(name = "token") val token: String = "",
        @Json(name = "admin_id") val adminId: String = "",
        @Json(name = "user_id") val id: String = "",
        val error: String = ""
    )

}

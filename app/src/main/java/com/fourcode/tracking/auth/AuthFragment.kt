package com.fourcode.tracking.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.auth_fragment.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        // Get default shared pref, for credentials
        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)

        // Logout if toLogout is true
        if (args.toLogout) sharedPreferences.edit {
            remove(getString(R.string.shared_pref_token))
        }

        // Check if logged in first
        val isLoggedIn = sharedPreferences.getString(
            getString(R.string.shared_pref_token), null
        ) != null

        // Skip login screen if isLoggedIn is true
        if (isLoggedIn) findNavController()
            .navigate(AuthFragmentDirections.startStandard())

        login_button.setOnClickListener {
            val username = username_input.text.toString()
            val password = password_input.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) launch {
                isLoading(true)

                val response = startStandardLogin(username, password)
                if (response.status == "success") {
                    Timber.d("Login ${response.status}")

                    // Save credentials locally (this means user is logged in)
                    sharedPreferences.edit {
                        // Will not crash, successful call always have data
                        putString(getString(R.string.shared_pref_token), response.data!!.token)
                    }

                    // Open next screen after token is saved
                    findNavController().navigate(
                        AuthFragmentDirections.startStandard())

                } else
                // Show error if error is not empty
                    Snackbar.make(
                        view,
                        response.message,
                        Snackbar.LENGTH_SHORT
                    ).show()
                isLoading(false)

                // Log error
                Timber.w("Orbit API Error Message: ${response.message}")
            } else Snackbar.make(
                view,
                R.string.error_missing_fields,
                Snackbar.LENGTH_SHORT
            ).show()

        }
    }

    private fun isLoading(value: Boolean) {
        // Set up progress bar if yes
        progress_bar.visibility =
            if (value) View.VISIBLE else View.INVISIBLE

        // Enable UI components
        username_input.isEnabled = value.not()
        password_input.isEnabled = value.not()
        login_button.isEnabled = value.not()
    }

    private suspend fun startStandardLogin(
        username: String, password: String
    ): LoginResponse {
        // Create an okhttp3
        val client = OkHttpClient()

        val body = """
            { "username": "$username", "password": "$password" }
        """.trimIndent().toRequestBody()

        // Create OkHttpRequest
        val request = Request.Builder()
            .url(BuildConfig.AuthApiUrl)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            // Create json object (kxs)
            val json = Json(JsonConfiguration.Stable)

            // Execute request and get response
            val response = client.newCall(request).execute()
            val responseBody = response.body!!.string()

            // Log response body
            Timber.d("ResponseBody: $responseBody")

            // Parse result before closing response
            val result = json.parse(LoginResponse.serializer(), responseBody)

            // Close response to avoid leaks
            response.close()

            return@withContext result
        }
    }

    override fun onDestroy() {
        // Cancel all running co-routines
        job.cancel(); super.onDestroy()
    }

    @Serializable
    private data class LoginResponse(
        val status: String,
        val message: String = "",
        val data: LoginData? = null
    )

    @Serializable
    private data class LoginData(
        val token: String
    )

}

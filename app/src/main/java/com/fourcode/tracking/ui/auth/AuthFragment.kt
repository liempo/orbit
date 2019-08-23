package com.fourcode.tracking.ui.auth

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.beust.klaxon.Klaxon
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import kotlinx.android.synthetic.main.auth_fragment.*
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.CoroutineContext

class AuthFragment : Fragment(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.auth_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize shared preferences object, for credentials
        val sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.shared_pref_credentials), MODE_PRIVATE)

        // Check if logged in first
        val loggedInId = sharedPreferences.getString(
            getString(R.string.shared_pref_id), null)
        if (loggedInId.isNullOrBlank().not())
            findNavController().navigate(AuthFragmentDirections.startMap())

        login_button.setOnClickListener {
            val username = username_input.text.toString()
            val password = password_input.text.toString()

            // Disable error first
            username_input_layout.isErrorEnabled = false
            password_input_layout.isErrorEnabled = false

            // Check first if fields are empty
            if (username.isBlank()) username_input_layout.error =
                    getString(R.string.error_field_required)
            if (password.isBlank()) password_input_layout.error =
                getString(R.string.error_field_required)

            if (username.isBlank().not() && password.isBlank().not()) launch {
                // Disable UI components while call
                username_input_layout.isEnabled = false
                password_input_layout.isEnabled = false
                login_button.isEnabled = false

                // Run call and get response with context
                val response =  login(username, password)

                // Show error if error is not empty
                if (response.error.isNotEmpty())
                    Toast.makeText(context,
                        response.error,
                        Toast.LENGTH_LONG).show()
                else {
                    // Save credentials locally (this means user is logged in)
                    sharedPreferences.edit {
                        putString(getString(R.string
                            .shared_pref_token), response.token)
                        putString(getString(R.string
                            .shared_pref_id), response.id)
                    }

                    // Start mapFragment
                    findNavController().navigate(R.id.mapFragment)
                }

                // Enable UI componente
                username_input_layout.isEnabled = true
                password_input_layout.isEnabled = true
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
            .url(BuildConfig.TrackingApiBaseUrl + "login")
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
        job.cancel() // Cancels all jobs
        super.onDestroy()
    }

    private data class LoginResponse(
        val token: String = "",
        val id: String = "",
        val error: String = ""
    )
}

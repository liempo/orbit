package com.fourcode.tracking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.beust.klaxon.Klaxon
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import kotlinx.android.synthetic.main.auth_fragment.*
import okhttp3.*
import timber.log.Timber
import java.io.IOException

class AuthFragment : Fragment(), Callback {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.auth_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

            if (username.isBlank().not() && password.isBlank().not())
                login(username, password)
        }
    }

    private fun login(username: String, password: String) {
        // Create an okhttp3
        val client = OkHttpClient()

        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url(BuildConfig.TrackingApiBaseUrl + "login")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(this)
    }

    override fun onResponse(call: Call, response: Response) {
        // Parse JSON object ot kotlin data class
        val r = Klaxon().parse<LoginResponse>(
            response.body!!.string()) ?: return

        // Show error if error is not empty
        if (r.error.isNotEmpty()) activity?.runOnUiThread {
            Toast.makeText(context, r.error,
                Toast.LENGTH_LONG).show()
        } else {
            val action = AuthFragmentDirections
                .startMap(r.token, r.id)
            findNavController().navigate(action)
        }
    }

    override fun onFailure(call: Call, e: IOException) {
        Timber.e(e)
    }

    private data class LoginResponse(
        val token: String = "",
        val id: String = "",
        val error: String = ""
    )


}

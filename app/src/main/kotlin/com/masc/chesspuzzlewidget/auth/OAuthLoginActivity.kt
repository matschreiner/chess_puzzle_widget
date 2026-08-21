package com.masc.chesspuzzlewidget.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.masc.chesspuzzlewidget.R
import com.masc.chesspuzzlewidget.widget.ChessPuzzleWidgetProvider
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

/** The only foreground screen in the app: hosts the OAuth2 PKCE login flow against Lichess. */
class OAuthLoginActivity : AppCompatActivity() {

    private lateinit var authService: AuthorizationService
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var loginButton: Button

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onAuthorizationResult(result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth_login)

        authService = AuthorizationService(this)
        statusText = findViewById(R.id.login_status_text)
        progress = findViewById(R.id.login_progress)
        loginButton = findViewById(R.id.login_button)

        loginButton.setOnClickListener { startLogin() }
    }

    override fun onDestroy() {
        authService.dispose()
        super.onDestroy()
    }

    private fun startLogin() {
        setBusy(getString(R.string.login_activity_title))

        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(OAuthConfig.AUTHORIZATION_ENDPOINT),
            Uri.parse(OAuthConfig.TOKEN_ENDPOINT)
        )
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            OAuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(OAuthConfig.REDIRECT_URI)
        ).setScope(OAuthConfig.SCOPE).build()

        authLauncher.launch(authService.getAuthorizationRequestIntent(request))
    }

    private fun onAuthorizationResult(data: Intent?) {
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        val exception = data?.let { AuthorizationException.fromIntent(it) }

        if (response == null) {
            setIdle(getString(R.string.login_failed))
            return
        }

        val authState = AuthState(response, exception)
        authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
            authState.update(tokenResponse, tokenException)
            if (tokenResponse == null) {
                runOnUiThread { setIdle(getString(R.string.login_failed)) }
                return@performTokenRequest
            }
            TokenStore(applicationContext).saveAuthState(authState)
            ChessPuzzleWidgetProvider.forceFetchAllWidgets(applicationContext)
            runOnUiThread { finish() }
        }
    }

    private fun setBusy(message: String) {
        statusText.text = message
        progress.visibility = View.VISIBLE
        loginButton.visibility = View.GONE
    }

    private fun setIdle(message: String) {
        statusText.text = message
        progress.visibility = View.GONE
        loginButton.visibility = View.VISIBLE
    }
}

package com.example.blank.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(private val context: Context) {

    private val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        }
    }

    fun isSignedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    fun signedInEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val accountObj = account.account ?: return@withContext null
        val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
        GoogleAuthUtil.getToken(context, accountObj, scope)
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            signInClient.signOut()
        }
    }

    fun revokeAccess() {
        signInClient.revokeAccess()
    }
}

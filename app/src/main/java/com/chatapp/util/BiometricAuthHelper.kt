package com.chatapp.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AuthResult(
        val success: Boolean,
        val errorMessage: String? = null
    )

    fun canAuthenticate(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onResult: (AuthResult) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock API Key")
            .setSubtitle("Confirm your identity to view the API key")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setConfirmationRequired(false)
            .build()

        val prompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(AuthResult(success = true))
            }

            override fun onAuthenticationFailed() {
                // Called on each failed biometric attempt — ignore,
                // wait for onAuthenticationError for the final result.
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onResult(AuthResult(success = false))
                    return
                }
                onResult(AuthResult(success = false, errorMessage = errString.toString()))
            }
        })
        prompt.authenticate(promptInfo)
    }
}

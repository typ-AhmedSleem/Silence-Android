package org.smssecure.smssecure

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class BiometricLockActivity : AppCompatActivity(R.layout.activity_biometric_lock) {

	private lateinit var tvPrompt: MaterialTextView
	private lateinit var lottieFingerprint: LottieAnimationView

	@OptIn(DelicateCoroutinesApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		tvPrompt = findViewById(R.id.tv_prompt)
		lottieFingerprint = findViewById(R.id.lottie)

		val biometricManager = BiometricManager.from(this)
		when (biometricManager.canAuthenticate()) {
			BiometricManager.BIOMETRIC_SUCCESS -> {
				val executor = ContextCompat.getMainExecutor(this)
				val callback = object : BiometricPrompt.AuthenticationCallback() {
					override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
						tvPrompt.text = errString
						playLottieAnimation(R.raw.lottie_fingerprint_to_error)
					}

					override fun onAuthenticationFailed() {
						tvPrompt.text = getString(R.string.fingerprint_auth_failed)
						playLottieAnimation(R.raw.lottie_fingerprint_to_error)
					}

					override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
						tvPrompt.visibility = View.INVISIBLE
						playLottieAnimation(R.raw.lottie_fingerprint_to_success)
						GlobalScope.launch(Dispatchers.IO) {
							delay(2.seconds)
							withContext(Dispatchers.Main) { proceedToMain() }
						}
					}
				}

				val biometricPrompt = BiometricPrompt(this, executor, callback)
				val promptInfo = BiometricPrompt.PromptInfo.Builder()
					.setTitle(getString(R.string.biometric_login_required))
					.setSubtitle(getString(R.string.msg_biometric_prompt))
					.setDeviceCredentialAllowed(true)
					.build()
				biometricPrompt.authenticate(promptInfo)
			}

			BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
				tvPrompt.text = getString(R.string.can_t_use_fingerprint_right_now_try_again_later)
				playLottieAnimation(R.raw.lottie_fingerprint_to_error)
			}

			BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
			BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
				proceedToMain()
			}

		}
	}

	private fun playLottieAnimation(lottieRawRes: Int) {
		lottieFingerprint.progress = 0f
		lottieFingerprint.setAnimation(lottieRawRes)
		lottieFingerprint.playAnimation()
	}

	private fun proceedToMain() {
		startActivity(Intent(this, ConversationListActivity::class.java))
		finish()
	}

}
package org.smssecure.smssecure

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smssecure.smssecure.util.SilencePreferences
import org.smssecure.smssecure.util.SilenceTrial
import kotlin.time.Duration.Companion.seconds

class SplashScreenActivity : AppCompatActivity() {
	@OptIn(DelicateCoroutinesApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_dummy_splash)
		if (supportActionBar != null) supportActionBar!!.hide()

		GlobalScope.launch(Dispatchers.IO) {
			delay(2.seconds)
			withContext(Dispatchers.Main) {
				val expired = SilenceTrial.checkTrialExpired(this@SplashScreenActivity)
				if (expired) {
					Log.w("SilenceTrial", "Your trial has expired.")
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAndRemoveTask()
					else finish()
					return@withContext
				}
				val target = if (SilencePreferences.isBiometricLockEnabled(this@SplashScreenActivity)) {
					Log.i("BiometricLock", "Biometric lock is enabled")
					val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
					if (keyguardManager.isKeyguardSecure) BiometricLockActivity::class.java
					else ConversationListActivity::class.java
				} else {
					Log.i("BiometricLock", "Biometric lock is disabled")
					ConversationListActivity::class.java
				}
				startActivity(Intent(this@SplashScreenActivity, target))
				finish()
			}
		}
	}
}
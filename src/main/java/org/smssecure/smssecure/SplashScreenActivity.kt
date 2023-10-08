package org.smssecure.smssecure

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
				val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
				val target = if (keyguardManager.isKeyguardSecure) BiometricLockActivity::class.java
				else ConversationListActivity::class.java
				startActivity(Intent(this@SplashScreenActivity, target))
				finish()
			}
		}
	}
}
package org.smssecure.smssecure.util

import android.app.Activity
import android.os.Build
import android.util.Log
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

object SilenceTrial {

	@JvmStatic
	fun checkTrialExpired(activity: Activity): Boolean {
		val deadline = Calendar.getInstance().apply { set(2023, Calendar.OCTOBER, 9, 23, 59) }
		val now = Calendar.getInstance()
		if (now.after(deadline)) return true
		else Log.w("SilenceTrial", "User still have [${(deadline.timeInMillis - now.timeInMillis).milliseconds} | ${deadline.timeInMillis - now.timeInMillis}] remaining.")
		return false
	}

}
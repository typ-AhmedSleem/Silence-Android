package org.smssecure.smssecure.util

import android.app.Activity
import android.os.Build
import android.util.Log
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

object TimedAppUsage {

	@JvmStatic
	fun checkTimedUsage(activity: Activity) {
		val deadline = Calendar.getInstance().apply { set(2023, Calendar.OCTOBER, 9, 11, 59) }
		val now = Calendar.getInstance()
		if (now.after(deadline)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) activity.finishAndRemoveTask()
			else activity.finish()
		} else Log.w("TimedAppUsage", "checkTimedUsage: User still have ${(deadline.timeInMillis - now.timeInMillis).milliseconds} remaining.")
	}

}
package org.smssecure.smssecure.util

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import java.io.Serializable

fun <T : Parcelable> Intent.getParcelable(name: String, type: Class<T>): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) this.getParcelableExtra(name, type)
	else this.getParcelableExtra(name)
}

fun <T : Serializable> Intent.getSerializable(name: String, type: Class<T>): Serializable? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) this.getSerializableExtra(name, type)
	else this.getSerializableExtra(name)
}

fun <T> Array<T>.takeUntil(limit: Int): Array<T> {
	return if (this.size >= limit) this.sliceArray(0 until limit)
	else this  // Empty array
}
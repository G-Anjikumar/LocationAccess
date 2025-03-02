package com.anji.locationaccess.util

import android.util.Patterns

fun String.isEmailValid(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()
fun String.isPasswordValid(): Boolean = this.length > 4
package com.anji.locationaccess

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anji.locationaccess.data.local.repo.user.UserState
import com.anji.locationaccess.data.viewmodel.UserDataViewModel
import com.anji.locationaccess.databinding.ActivityUserLoginBinding
import com.anji.locationaccess.util.isEmailValid
import com.anji.locationaccess.util.isPasswordValid
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserLoginActivity : AppCompatActivity() {

    private lateinit var userLoginBinding: ActivityUserLoginBinding
    private val loginViewModel: UserDataViewModel by viewModels()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userLoginBinding = ActivityUserLoginBinding.inflate(LayoutInflater.from(this))
        setContentView(userLoginBinding.root)
        userLoginBinding.login.setOnClickListener {
            val userState = UserState(
                name = userLoginBinding.userIdTIE.text.toString(),
                password = userLoginBinding.passwordTIE.text.toString()
            )
//            loginViewModel.createUser(userState)
           /* coroutineScope.launch {
                loginViewModel.userState.collectLatest {
                    Log.d("Login State", "LoginDeails$it")
                }
            }*/
        }
    }

    private fun checkFields(): Boolean {
        return if (userLoginBinding.userIdTIE.text.toString() == "" &&
            TextUtils.isEmpty(userLoginBinding.userIdTIE.text.toString()) &&
            userLoginBinding.emailTIE.text.toString().isEmailValid()
        ) {
            Snackbar.make(userLoginBinding.emailTIE, "Invalid Email", 5).show()
            false
        } else {
            true
        }
        return if (userLoginBinding.passwordTIE.text.toString() == "" &&
            TextUtils.isEmpty(userLoginBinding.passwordTIE.text.toString()) && userLoginBinding.passwordTIE.text.toString()
                .isPasswordValid()
        ) {
            Snackbar.make(userLoginBinding.passwordTIE, "Invalid Password", 5).show()
            false
        } else {
            true
        }
    }
}
package dev.samadali.zen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.SignInButton

class LoginActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val googleSignInButton = findViewById<SignInButton>(R.id.btn_google_sign_in)
        googleSignInButton.setSize(SignInButton.SIZE_WIDE)
    }
}
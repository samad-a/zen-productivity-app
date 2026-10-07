package dev.samadali.zen.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.samadali.zen.databinding.ActivitySignupBinding

// TODO: implement account creation; the sign up button currently does nothing
class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySignupBinding.inflate(layoutInflater).root)
    }
}

package dev.samadali.zen.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.SignInButton
import dev.samadali.zen.MainActivity
import dev.samadali.zen.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)

        // TODO: authenticate the user; for now logging in goes straight into the app
        binding.loginButton.setOnClickListener {
            // Clear the landing/login screens so back from the app doesn't return to them
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}

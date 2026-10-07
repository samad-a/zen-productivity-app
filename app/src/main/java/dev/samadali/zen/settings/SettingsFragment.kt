package dev.samadali.zen.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dev.samadali.zen.R
import dev.samadali.zen.auth.LandingActivity
import dev.samadali.zen.databinding.FragmentSettingsBinding
import dev.samadali.zen.pomodoro.PomodoroTimer

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)

        binding.logoutButton.setOnClickListener {
            PomodoroTimer.pause()
            val intent = Intent(requireContext(), LandingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}

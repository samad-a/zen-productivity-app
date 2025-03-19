package dev.samadali.zen

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class PomodoroViewModel : androidx.lifecycle.ViewModel() {
    private val _currentTimeInMillis = MutableLiveData(0L)
    val currentTimeInMillis: LiveData<Long> = _currentTimeInMillis

    private val _totalTimeInMillis = MutableLiveData(0L)
    val totalTimeInMillis: LiveData<Long> = _totalTimeInMillis

    private val _isTimerRunning = MutableLiveData(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    private val _isStudyTime = MutableLiveData(true)
    val isStudyTime: LiveData<Boolean> = _isStudyTime

    var timer: CountDownTimer? = null
    var studyDuration = 25L
    var breakDuration = 5L

    init {
        // Initialize timer if it's not already running
        if (!_isTimerRunning.value!! && _currentTimeInMillis.value == 0L) {
            _currentTimeInMillis.value = studyDuration * 60 * 1000
            _totalTimeInMillis.value = _currentTimeInMillis.value
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value == true) return

        val duration = if (_isStudyTime.value == true) {
            studyDuration
        } else {
            breakDuration
        }

        if (_currentTimeInMillis.value == 0L) {
            _totalTimeInMillis.value = duration * 60 * 1000
            _currentTimeInMillis.value = _totalTimeInMillis.value
        }

        timer = object : CountDownTimer(_currentTimeInMillis.value!!, 100) { // Update every 100ms for smoother animation
            override fun onTick(millisUntilFinished: Long) {
                _currentTimeInMillis.value = millisUntilFinished
            }

            override fun onFinish() {
                _isStudyTime.value = !(_isStudyTime.value ?: true)
                _currentTimeInMillis.value = 0L
                startTimer() // Automatically start the next phase
            }
        }.start()

        _isTimerRunning.value = true
    }

    fun stopTimer() {
        timer?.cancel()
        _isTimerRunning.value = false
    }

    fun updateStudyDuration(minutes: Long) {
        studyDuration = minutes
        if (_isStudyTime.value == true && _isTimerRunning.value != true) {
            _currentTimeInMillis.value = minutes * 60 * 1000
            _totalTimeInMillis.value = _currentTimeInMillis.value
        }
    }

    fun updateBreakDuration(minutes: Long) {
        breakDuration = minutes
        if (_isStudyTime.value != true && _isTimerRunning.value != true) {
            _currentTimeInMillis.value = minutes * 60 * 1000
            _totalTimeInMillis.value = _currentTimeInMillis.value
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

class Pomodoro : Fragment() {
    private val viewModel: PomodoroViewModel by activityViewModels()
    private lateinit var clockTimer: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var startStopButton: Button
    private lateinit var studyTextInput: TextInputEditText
    private lateinit var breakTextInput: TextInputEditText
    private var progressAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pomodoro, container, false)
        
        // Initialize views
        clockTimer = view.findViewById(R.id.clockTimer)
        progressBar = view.findViewById(R.id.progressBar)
        startStopButton = view.findViewById(R.id.startStopButton)
        studyTextInput = view.findViewById(R.id.studyTextInput)
        breakTextInput = view.findViewById(R.id.breakTextInput)

        // Set up observers
        viewModel.currentTimeInMillis.observe(viewLifecycleOwner, Observer { time ->
            updateTimerUI(time, viewModel.totalTimeInMillis.value ?: time)
        })

        viewModel.isTimerRunning.observe(viewLifecycleOwner, Observer { isRunning ->
            startStopButton.text = if (isRunning) "Stop" else "Start"
        })

        viewModel.isStudyTime.observe(viewLifecycleOwner, Observer { isStudy ->
            // Update input fields based on current phase
            if (isStudy) {
                studyTextInput.setText((viewModel.currentTimeInMillis.value!! / 1000 / 60).toString())
            } else {
                breakTextInput.setText((viewModel.currentTimeInMillis.value!! / 1000 / 60).toString())
            }
        })

        // Set initial values
        studyTextInput.setText(viewModel.studyDuration.toString())
        breakTextInput.setText(viewModel.breakDuration.toString())

        // Set up text change listeners
        studyTextInput.addTextChangedListener(createTextWatcher(true))
        breakTextInput.addTextChangedListener(createTextWatcher(false))

        // Set up click listener for start/stop button
        startStopButton.setOnClickListener {
            if (viewModel.isTimerRunning.value == true) {
                viewModel.stopTimer()
            } else {
                viewModel.startTimer()
            }
        }

        return view
    }

    private fun createTextWatcher(isStudy: Boolean): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.isTimerRunning.value != true) {
                    val minutes = s.toString().toLongOrNull() ?: 0L
                    if (minutes > 0) {
                        if (isStudy) {
                            viewModel.updateStudyDuration(minutes)
                        } else {
                            viewModel.updateBreakDuration(minutes)
                        }
                    }
                }
            }
        }
    }

    private fun updateTimerUI(currentTime: Long, totalTime: Long) {
        val minutes = (currentTime / 1000) / 60
        val seconds = (currentTime / 1000) % 60
        clockTimer.text = String.format("%02d:%02d", minutes, seconds)

        val progress = ((totalTime - currentTime) * 100 / totalTime).toInt()
        
        // Cancel any existing animation
        progressAnimator?.cancel()
        
        // Create and start new animation
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progress).apply {
            duration = 100 // Match the timer update interval
            setAutoCancel(true)
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any running animation when the view is destroyed
        progressAnimator?.cancel()
        progressAnimator = null
    }
}
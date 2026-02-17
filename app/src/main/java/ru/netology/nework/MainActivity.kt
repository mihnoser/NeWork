package ru.netology.nework

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.BounceInterpolator
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var drawTextView: DrawTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawTextView = findViewById(R.id.drawTextView)
        drawTextView.setStrokeColor(Color.parseColor("#2196F3"))
        drawTextView.setFillColor(Color.parseColor("#1976D2"))
        drawTextView.setStrokeWidth(6f)
        drawTextView.setTextSize(140f)
        drawTextView.onAnimationComplete = {
            runOnUiThread {
                navigateToMainScreen()
            }
        }

        startEntranceAnimation()
    }

    private fun startEntranceAnimation() {
        drawTextView.alpha = 0f
        drawTextView.scaleX = 0.8f
        drawTextView.scaleY = 0.8f

        drawTextView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                Handler(Looper.getMainLooper()).postDelayed({
                }, 500)
            }
            .start()
    }

    private fun navigateToMainScreen() {
        android.widget.Toast.makeText(this, "Добро пожаловать в NeWork!", android.widget.Toast.LENGTH_LONG).show()
    }
}
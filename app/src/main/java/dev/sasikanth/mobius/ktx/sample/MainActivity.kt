package dev.sasikanth.mobius.ktx.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.spotify.mobius.Connection
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.MobiusAndroid
import dev.sasikanth.mobius.ktx.FlowMobius
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.LazyThreadSafetyMode.NONE

interface UiActions {
  fun showBelowZeroError()
}

class MainActivity : AppCompatActivity(), UiActions {

  companion object {
    private const val COUNTER_KEY = "counter_key"
  }

  private val loop by lazy(NONE) {
    FlowMobius
      .loop(
        CounterUpdate(),
        CounterEffectHandler(this).build()
      )
  }

  private lateinit var controller: MobiusLoop.Controller<Int, CounterEvent>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    controller = MobiusAndroid.controller(loop, 0)
    controller.connect { eventConsumer ->
      object : Connection<Int> {
        override fun accept(value: Int) {
          counterText.text = value.toString()

          increment.setOnClickListener {
            eventConsumer.accept(Increment)
          }

          decrement.setOnClickListener {
            eventConsumer.accept(Decrement)
          }
        }

        override fun dispose() {
          increment.setOnClickListener(null)
          decrement.setOnClickListener(null)
        }
      }
    }

    if (savedInstanceState != null) {
      controller.replaceModel(savedInstanceState.getInt(COUNTER_KEY))
    }
  }

  override fun onResume() {
    super.onResume()
    controller.start()
  }

  override fun onPause() {
    super.onPause()
    controller.stop()
  }

  override fun onDestroy() {
    super.onDestroy()
    controller.disconnect()
  }

  override fun showBelowZeroError() {
    Snackbar.make(mainRoot, "Can't decrement below zero", Snackbar.LENGTH_SHORT).show()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(COUNTER_KEY, controller.model)
  }
}

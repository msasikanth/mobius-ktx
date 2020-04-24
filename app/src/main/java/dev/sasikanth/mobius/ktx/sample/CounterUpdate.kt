package dev.sasikanth.mobius.ktx.sample

import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

class CounterUpdate : Update<Int, CounterEvent, CounterEffect> {
  override fun update(model: Int, event: CounterEvent): Next<Int, CounterEffect> {
    return when (event) {
      Increment -> next(model + 1)
      Decrement -> {
        val newModel = model - 1
        if (newModel < 0) {
          dispatch<Int, CounterEffect>(setOf(ShowBelowZeroError))
        } else {
          next<Int, CounterEffect>(newModel)
        }
      }
    }
  }
}

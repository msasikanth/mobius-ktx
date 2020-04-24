package dev.sasikanth.mobius.ktx.sample

import dev.sasikanth.mobius.ktx.FlowMobius
import dev.sasikanth.mobius.ktx.FlowTransformer
import kotlinx.coroutines.Dispatchers

class CounterEffectHandler(
  private val uiActions: UiActions
) {

  fun build(): FlowTransformer<CounterEffect, CounterEvent> {
    return FlowMobius
      .subtypeEffectHandler<CounterEffect, CounterEvent>()
      .addAction(ShowBelowZeroError::class, {
        uiActions.showBelowZeroError()
      }, Dispatchers.Main)
      .build()
  }
}

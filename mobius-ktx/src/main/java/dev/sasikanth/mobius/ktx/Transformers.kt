package dev.sasikanth.mobius.ktx

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

object Transformers {

  /**
   * Creates and [FlowTransformer] that will run an suspending function every time
   * an effect is received
   *
   * @param doEffect the suspending function to be run every time the effect is requested
   * @param dispatcher the [CoroutineContext] that the flow should flowOn
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an [FlowTransformer] that can be used with a [FlowMobius.SubtypeEffectHandlerBuilder].
   */
  @ExperimentalCoroutinesApi
  fun <F, E> fromAction(
    doEffect: suspend () -> Unit,
    dispatcher: CoroutineContext? = null
  ): FlowTransformer<F, E> = {
    val flow = flow<E> {
      collect { doEffect() }
    }
    if (dispatcher != null) flow.flowOn(dispatcher) else flow
  }

  /**
   * Creates and [FlowTransformer] that will run an suspending function every time
   * an effect is received
   *
   * @param doEffect the suspending function to be run every time the effect is requested
   * @param dispatcher the [CoroutineContext] that the flow should flowOn
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an [FlowTransformer] that can be used with a [FlowMobius.SubtypeEffectHandlerBuilder].
   */
  @ExperimentalCoroutinesApi
  fun <F, E> fromConsumer(
    doEffect: suspend (F) -> Unit,
    dispatcher: CoroutineContext? = null
  ): FlowTransformer<F, E> = {
    val flow = flow<E> {
      collect { effect -> doEffect(effect) }
    }
    if (dispatcher != null) flow.flowOn(dispatcher) else flow
  }
}

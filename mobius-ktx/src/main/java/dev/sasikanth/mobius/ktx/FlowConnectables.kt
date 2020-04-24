package dev.sasikanth.mobius.ktx

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Contains utility methods for converting back and forth between [FlowTransformer]s and
 * [Connectable]s.
 */
object FlowConnectables {

  @FlowPreview
  @ExperimentalCoroutinesApi
  fun <I, O> fromTransform(
    transformer: FlowTransformer<I, O>
  ): Connectable<I, O> {

    return Connectable<I, O> { output ->
      val job = SupervisorJob()
      val scope = CoroutineScope(job)

      val effectsChannel = ConflatedBroadcastChannel<I>()
      val effectsFlow = effectsChannel
        .asFlow()
        .let(transformer)

      scope.launch {
        effectsFlow.collect { output.accept(it) }
      }

      return@Connectable object : Connection<I> {
        override fun accept(effect: I) {
          effectsChannel.offer(effect)
        }

        override fun dispose() {
          job.cancel()
        }
      }
    }
  }
}

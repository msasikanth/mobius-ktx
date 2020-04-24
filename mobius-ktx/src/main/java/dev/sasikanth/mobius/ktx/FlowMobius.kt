package dev.sasikanth.mobius.ktx

import com.spotify.mobius.ConnectionException
import com.spotify.mobius.Mobius
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Factory methods for wrapping Mobius core classes with [Flow]
 */
object FlowMobius {

  /**
   * Create a [MobiusLoop.Builder] to help you configure a MobiusLoop before starting it.
   *
   * <p>Once done configuring the loop, you can start the loop using [MobiusLoop.Factory.startFrom].
   *
   * @param update the [Update] function of the loop
   * @param effectHandler the [FlowTransformer] effect handler of the loop
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @return a [MobiusLoop.Builder] instance that you can further configure before starting
   *     the loop
   */
  @FlowPreview
  @ExperimentalCoroutinesApi
  fun <M, E, F> loop(
    update: Update<M, E, F>,
    effectHandler: FlowTransformer<F, E>
  ): MobiusLoop.Builder<M, E, F> {
    return Mobius.loop(update, FlowConnectables.fromTransform(effectHandler))
  }

  /**
   * Create an [FlowMobius.SubtypeEffectHandlerBuilder] for handling effects based on their
   * type.
   *
   * @param <F> the effect type
   * @param <E> the event type
   */
  fun <F : Any, E> subtypeEffectHandler(): SubtypeEffectHandlerBuilder<F, E> {
    return SubtypeEffectHandlerBuilder()
  }

  /**
   * Builder for a type-routing effect handler.
   *
   * <p>Register handlers for different subtypes of F using the add(...) methods, and call [build]
   * to create an instance of the effect handler. You can then create a loop with the
   * router as the effect handler using [FlowMobius.loop].
   *
   * <p>The handler will look at the type of each incoming effect object and try to find a
   * registered handler for that particular subtype of F. If a handler is found, it will be given
   * the effect object, otherwise an exception will be thrown.
   *
   * <p>All the classes that the effect router know about must have a common type F. Note that
   * instances of the builder are mutable and not thread-safe.
   */
  @Suppress("MemberVisibilityCanBePrivate", "unused")
  class SubtypeEffectHandlerBuilder<F : Any, E> internal constructor() {

    private val effectPerformerMap = hashMapOf<Class<*>, Flow<F>.() -> Flow<E>>()

    /**
     * Add an [FlowTransformer] for handling effects of a given type. The handler will
     * receive all effect objects that extend the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param effectHandler the effect handler for the given effect class
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    @ExperimentalCoroutinesApi
    fun <G : F> addTransformer(
      effectClass: KClass<G>,
      effectHandler: Flow<F>.() -> Flow<E>
    ): SubtypeEffectHandlerBuilder<F, E> {
      for (cls in effectPerformerMap.keys) {
        require(!(cls.isAssignableFrom(effectClass::class.java) || effectClass::class.java.isAssignableFrom(cls))) {
          "Effect classes may not be assignable to each other, collision found: ${effectClass.simpleName} <-> ${cls.simpleName}"
        }
      }

      effectPerformerMap[effectClass::class.java] = {
        let(effectHandler)
          .catch { e ->
            throw ConnectionException("In effect handler: ${effectHandler.javaClass}", e)
          }
      }
      return this
    }

    /**
     * Add an suspended function for handling effects of a given type. The function will be invoked once
     * for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param action the function that should be invoked for the effect
     * @param dispatcher the coroutine context that should be used to invoke the action
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    @ExperimentalCoroutinesApi
    fun <G : F> addAction(
      effectClass: KClass<G>,
      action: suspend () -> Unit,
      dispatcher: CoroutineContext? = null
    ): SubtypeEffectHandlerBuilder<F, E> {
      return addTransformer(effectClass, Transformers.fromAction(action, dispatcher))
    }

    /**
     * Add an suspending function for handling effects of a given type. The function will be invoked
     * once for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param consumer the function that should be invoked for the effect
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    @ExperimentalCoroutinesApi
    fun <G : F> addConsumer(
      effectClass: KClass<G>,
      consumer: suspend (F) -> Unit,
      dispatcher: CoroutineContext? = null
    ): SubtypeEffectHandlerBuilder<F, E> {
      return addTransformer(effectClass, Transformers.fromConsumer(consumer, dispatcher))
    }

    @ExperimentalCoroutinesApi
    fun build(): FlowTransformer<F, E> {
      return {
        val effectClasses = effectPerformerMap.keys
        val effectTransformers = effectPerformerMap.values.toList()

        flow {
          collect { effect ->
            effectClasses
              .filterIsInstance(effect::class.java)
              .map {
                throw UnknownEffectException(it)
              }

            val transformed = effectTransformers
              .map {
                flowOf(effect).let(it)
              }

            this.emitAll(transformed.merge())
          }
        }
      }
    }
  }
}

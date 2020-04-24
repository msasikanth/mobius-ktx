package dev.sasikanth.mobius.ktx

import kotlinx.coroutines.flow.Flow

/**
 * Extension function which provides a functional format to take a [Flow] of
 * input type and transform into output type.
 * @param <I>: Input
 * @param <O>: Output
 */
typealias FlowTransformer<I, O> = Flow<I>.() -> Flow<O>

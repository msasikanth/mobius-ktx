package dev.sasikanth.mobius.ktx.sample

sealed class CounterEvent

object Increment : CounterEvent()

object Decrement : CounterEvent()

package dev.sasikanth.mobius.ktx.sample

sealed class CounterEffect

object ShowBelowZeroError : CounterEffect()

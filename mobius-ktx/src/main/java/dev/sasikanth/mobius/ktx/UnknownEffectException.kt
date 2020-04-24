package dev.sasikanth.mobius.ktx

/**
 * Indicates that effect handler received an effect that it hasn't been configured to handle.
 * This is a programmer error
 */
class UnknownEffectException(
  private val effect: Any
) : RuntimeException() {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || this.javaClass != other.javaClass) {
      return false
    }

    val that = other as UnknownEffectException
    return effect == that.effect
  }

  override fun hashCode(): Int {
    return effect.hashCode()
  }
}

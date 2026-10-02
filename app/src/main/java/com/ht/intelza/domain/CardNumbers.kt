package com.ht.intelza.domain

/** Card numbers are AprilTag ids from the tag36h11 family (587 codes; 0 is not used). */
object CardNumbers {
    const val MIN = 1
    const val MAX = 586

    fun isValid(number: Int): Boolean = number in MIN..MAX

    /** The lowest card number not in [used]. */
    fun nextFree(used: Collection<Int>): Int? {
        val taken = used.toHashSet()
        return (MIN..MAX).firstOrNull { it !in taken }
    }
}

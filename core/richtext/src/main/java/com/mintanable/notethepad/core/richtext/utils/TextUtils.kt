package com.mintanable.notethepad.core.richtext.utils

object TextUtils {

     fun findTextChangeStart(old: String, new: String): Int {
        val minLen = minOf(old.length, new.length)
        for (i in 0 until minLen) {
            if (old[i] != new[i]) return i
        }
        return minLen
    }

     fun findTextChangeEnd(old: String, new: String, changeStart: Int): Int {
        val oldLen = old.length
        val newLen = new.length
        val maxFromEnd = minOf(oldLen - changeStart, newLen - changeStart)
        for (i in 1..maxFromEnd) {
            if (old[oldLen - i] != new[newLen - i]) return oldLen - i + 1
        }
        // No diff found from the end: the deleted/inserted chars are all at changeStart.
        // Return changeStart + deleted count so spans at changeStart shift correctly.
        return changeStart + maxOf(0, oldLen - newLen)
    }
}
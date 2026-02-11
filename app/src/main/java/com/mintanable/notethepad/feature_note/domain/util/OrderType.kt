package com.mintanable.notethepad.features.domain.util

sealed class OrderType{
    object Ascending: OrderType()
    object Descending: OrderType()
}

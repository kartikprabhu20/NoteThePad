package com.mintanable.notethepad.core.model.note

sealed class NoteOrder(open val orderType: OrderType) {
    data class Title(override val orderType: OrderType): NoteOrder(orderType)
    data class Color(override val orderType: OrderType): NoteOrder(orderType)
    data class Date(override val orderType: OrderType): NoteOrder(orderType)

    fun copyOrder(newOrderType: OrderType): NoteOrder {
        return when (this) {
            is Title -> this.copy(orderType = newOrderType)
            is Color -> this.copy(orderType = newOrderType)
            is Date -> this.copy(orderType = newOrderType)
        }
    }
}

sealed class OrderType {
    object Ascending : OrderType()
    object Descending : OrderType()
}

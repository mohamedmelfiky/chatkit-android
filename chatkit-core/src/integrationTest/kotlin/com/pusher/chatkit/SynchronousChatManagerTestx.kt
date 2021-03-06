package com.pusher.chatkit

import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.util.FutureValue

/**
 * Waits for connection to be stabilised before waiting for a given event
 */
fun <A> SynchronousChatManager.connectFor(block: (ChatEvent) -> A?): FutureValue<A> {
    val futureValue = FutureValue<A>()
    var ready by FutureValue<Any>()
    connect {
        if (it is ChatEvent.CurrentUserReceived) ready = it
        (block(it))?.let { futureValue.set(it) }
    }
    checkNotNull(ready)
    return futureValue
}

/**
 * Same as [connectFor] but for room subs
 */
fun <A> SynchronousChatManager.subscribeRoomFor(roomName: String, block: (RoomEvent) -> A?): FutureValue<A> {
    val currentUserEvent by connectFor { it as? ChatEvent.CurrentUserReceived }
    return currentUserEvent.currentUser.subscribeRoomFor(roomName, block)
}

/**
 * Same as [connectFor] but for room subs
 */
fun <A> SynchronousCurrentUser.subscribeRoomFor(roomName: String, block: (RoomEvent) -> A?): FutureValue<A> {
    val futureValue = FutureValue<A>()
    val room = rooms.first { it.name == roomName }
    subscribeToRoomMultipart(room) {
        (block(it))?.let { futureValue.set(it) }
    }
    return futureValue
}

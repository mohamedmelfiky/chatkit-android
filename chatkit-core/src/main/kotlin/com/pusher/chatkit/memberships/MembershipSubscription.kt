package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.platform.SubscriptionListeners
import elements.Subscription
import elements.SubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent.*
import java.util.concurrent.Future
import com.pusher.util.*
import elements.Errors
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.waitOr
import elements.Error
import java.util.concurrent.TimeUnit.SECONDS

internal class MembershipSubscription(
    private val roomId: Int,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private var active = false
    private val logger = chatManager.dependencies.logger
    private val roomStore = chatManager.roomService.roomStore
    private val subscription = chatManager.subscribeNonResuming(
        path = "/rooms/$roomId/memberships",
        listeners = SubscriptionListeners(
            onOpen = { headers ->
                active = true
                logger.verbose("[Membership subscription] OnOpen $headers")
            },
            onEvent = { event: SubscriptionEvent<MembershipSubscriptionEvent> ->
                event.body
                    .applySideEffects()
                    .toChatManagerEvent()
                    .waitOr(Wait.For(10, SECONDS)) { ErrorOccurred(Errors.other(it)).asSuccess() }
                    .recover { ErrorOccurred(it) }
                    .also(consumeEvent)
                    .also { logger.verbose("Event received $event") }
            },
            onError = { error -> consumeEvent(ChatManagerEvent.ErrorOccurred(error))},
            onSubscribe = { logger.verbose("Subscription established") },
            onRetrying = { logger.verbose("Subscription lost. Trying again.") },
            onEnd = { error -> logger.verbose("Subscription ended with: $error") }
        ),
        messageParser = MembershipSubscriptionEventParser
    )

    override fun unsubscribe() {
        active = false
        subscription.unsubscribe()
    }

    private fun MembershipSubscriptionEvent.applySideEffects(): MembershipSubscriptionEvent = this.apply {
        when (this) {
            is InitialState -> {
                userIds.forEach { userId -> roomStore[roomId]?.addUser(userId) }
            }
            is UserJoined -> {
                roomStore[roomId]?.addUser(userId)
            }
            is UserLeft -> {
                roomStore[roomId]?.removeUser(userId)
            }
        }
    }

    private fun MembershipSubscriptionEvent.toChatManagerEvent(): Future<Result<ChatManagerEvent, Error>> = when (this) {
        is InitialState -> {
            NoEvent.asSuccess<ChatManagerEvent, Error>().toFuture()
        }
        is UserLeft -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is UserJoined -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
    }
}
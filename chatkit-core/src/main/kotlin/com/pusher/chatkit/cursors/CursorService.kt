package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import java.net.URLEncoder

class CursorService(
        private val client: PlatformClient,
        private val logger: Logger
) {
    private val cursorsStore = CursorsStore()

    private val setReadCursorThrottler =
            Throttler { options: RequestOptions ->
                client.doRequest<JsonElement>(
                        options = options,
                        responseParser = { it.parseAs() }
                )
            }

    fun setReadCursor(
        userId: String,
        roomId: String,
        position: Int
    ) = setReadCursorThrottler.throttle(
                RequestOptions(
                        method = "PUT",
                        path = "/cursors/0/rooms/$roomId/users/$userId",
                        body = """{ "position" : $position }"""
                )
        ).mapResult {
            cursorsStore[userId] += Cursor(
                    userId = userId,
                    roomId = roomId,
                    position = position
            )
        }

    fun getReadCursor(userId: String, roomId: String) : Result<Cursor, Error> =
        (cursorsStore[userId][roomId]?.asSuccess() ?: notSubscribedToRoom(roomId).asFailure())

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursors")

    fun subscribeForRoom(
            roomId: String,
            consumer: (ChatEvent) -> Unit
    ) = subscribe(
            "/cursors/0/rooms/${URLEncoder.encode(roomId, "UTF-8")}",
            consumer
    )

    fun subscribeForUser(
            userId: String,
            consumer: (ChatEvent) -> Unit
    ) = subscribe(
            "/cursors/0/users/${URLEncoder.encode(userId, "UTF-8")}",
            consumer
    )

    private fun subscribe(
            path: String,
            consumer: ChatManagerEventConsumer
    ) = ResolvableSubscription(
            client = client,
            path = path,
            listeners = SubscriptionListeners(
                    onEvent = { event -> cursorsStore.applyEvent(event.body).map(::enrichEvent).forEach(consumer) },
                    onError = { error -> cursorsStore.applyEvent(CursorSubscriptionEvent.OnError(error)).map(::enrichEvent).forEach(consumer) }
            ),
            messageParser = CursorSubscriptionEventParser,
            description = "Cursor user $path",
            logger = logger
    )

    private fun enrichEvent(event: CursorSubscriptionEvent): ChatEvent =
            when (event) {
                is CursorSubscriptionEvent.OnCursorSet -> ChatEvent.NewReadCursor(event.cursor)
                else -> ChatEvent.NoEvent
            }
}
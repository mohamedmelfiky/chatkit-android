package com.pusher.chatkit.users

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room


internal typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

internal sealed class UserSubscriptionEvent {

    internal data class InitialState(
            val currentUser: User,
            val rooms: List<RoomApiType>,
            val readStates: List<ReadStateApiType>,
            val memberships: List<RoomMembershipApiType>
    ) : UserSubscriptionEvent()

    internal data class AddedToRoomEvent(
            var room: RoomApiType,
            val readState: ReadStateApiType,
            val memberships: RoomMembershipApiType
    ) : UserSubscriptionEvent()

    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class RoomUpdatedEvent(val room: RoomApiType) : UserSubscriptionEvent()

    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class ReadStateUpdatedEvent(
            val readState: ReadStateApiType
    ) : UserSubscriptionEvent()

    internal data class UserJoinedRoomEvent(
            val userId: String,
            val roomId: String
    ) : UserSubscriptionEvent()

    internal data class UserLeftRoomEvent(
            val userId: String,
            val roomId: String
    ) : UserSubscriptionEvent()

    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}

internal data class RoomApiType(
        val id: String,
        val createdById: String,
        val name: String,
        val pushNotificationTitleOverride: String?,
        val private: Boolean,
        val customData: CustomData?,
        val lastMessageAt: String?,
        val createdAt: String,
        val updatedAt: String,
        val deletedAt: String?
)

internal data class ReadStateApiType(
        val roomId: String,
        val unreadCount: Int,
        val cursor: Cursor?
)

internal data class RoomMembershipApiType(val roomId: String, val userIds: List<String>)


internal sealed class UserInternalEvent {
    internal data class AddedToRoom(var room: Room) : UserInternalEvent()
    internal data class RemovedFromRoom(val roomId: String) : UserInternalEvent()
    internal data class RoomUpdated(val room: Room) : UserInternalEvent()
    internal data class RoomDeleted(val roomId: String) : UserInternalEvent()
    internal data class UserJoinedRoom(val userId: String, val roomId: String) : UserInternalEvent()
    internal data class UserLeftRoom(val userId: String, val roomId: String) : UserInternalEvent()
    internal data class NewCursor(val cursor: Cursor) : UserInternalEvent()
    internal data class ErrorOccurred(val error: elements.Error) : UserInternalEvent()
}

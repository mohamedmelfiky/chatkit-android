package com.pusher.chatkit.users.api

import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.state.JoinedRoomsStateDiffer
import com.pusher.chatkit.state.CurrentUserReceived
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.RoomUpdated
import org.reduxkotlin.Dispatcher

internal class UserSubscriptionDispatcher(
    private val userApiTypeMapper: UserApiTypeMapper,
    private val joinedRoomsStateDiffer: JoinedRoomsStateDiffer,
    private val joinedRoomApiTypeMapper: JoinedRoomApiTypeMapper,
    private val dispatcher: Dispatcher
) {

    internal fun onEvent(event: UserSubscriptionEvent) {
        when (event) {
            is UserSubscriptionEvent.InitialState -> {

                dispatcher(CurrentUserReceived(
                    userApiTypeMapper.toUserInternalType(event.currentUser)))

                if (joinedRoomsStateDiffer.stateExists()) {
                    joinedRoomsStateDiffer.toActions(
                        joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                        joinedRoomApiTypeMapper.toUnreadCounts(event.readStates)
                    ).forEach { action ->
                        dispatcher(action)
                    }
                } else {
                    dispatcher(
                        JoinedRoomsReceived(
                            joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                            joinedRoomApiTypeMapper.toUnreadCounts(event.readStates)
                        )
                    )
                }
            }
            is UserSubscriptionEvent.AddedToRoomEvent ->
                dispatcher(
                    JoinedRoom(
                        joinedRoomApiTypeMapper.toRoomInternalType(event.room),
                        event.readState.unreadCount
                    )
                )
            is UserSubscriptionEvent.RemovedFromRoomEvent ->
                dispatcher(LeftRoom(event.roomId))
            is UserSubscriptionEvent.RoomUpdatedEvent ->
                dispatcher(RoomUpdated(joinedRoomApiTypeMapper.toRoomInternalType(event.room)))
            is UserSubscriptionEvent.RoomDeletedEvent ->
                dispatcher(RoomDeleted(event.roomId))
        }
    }
}

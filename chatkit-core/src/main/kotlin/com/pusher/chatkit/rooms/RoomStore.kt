package com.pusher.chatkit.rooms

import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomApiType
import com.pusher.chatkit.users.RoomMembershipApiType
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import java.util.*
import kotlin.collections.LinkedHashMap

internal class RoomStore {
    private val rooms: MutableMap<String, RoomApiType> = Collections.synchronizedMap(LinkedHashMap())
    private val unreadCounts: MutableMap<String, Int> = Collections.synchronizedMap(LinkedHashMap())
    private val members: MutableMap<String, Set<String>> = Collections.synchronizedMap(LinkedHashMap())

    operator fun get(id: String): Room? =
            rooms[id]?.let { Room(it, members[id], unreadCounts[id]) }

    internal fun listAll(): List<Room> =
            rooms.keys.map { this[it]!! }

    internal fun clear() {
        rooms.clear()
        unreadCounts.clear()
        members.clear()
    }

    internal fun initialiseContents(rooms: List<RoomApiType>, memberships: List<RoomMembershipApiType>, readStates: List<ReadStateApiType>) {
        clear()
        rooms.forEach { this.rooms[it.id] = it }
        memberships.forEach { this.members[it.roomId] = it.userIds.toSet() }
        readStates.forEach { this.unreadCounts[it.roomId] = it.unreadCount }
    }

    private fun remove(roomId: String) {
        this.rooms.remove(roomId)
        this.members.remove(roomId)
        this.unreadCounts.remove(roomId)
    }

    fun applyUserSubscriptionEvent(
            event: UserSubscriptionEvent
    ): List<UserInternalEvent> =
            when (event) {
                is UserSubscriptionEvent.InitialState -> {
                    val addedRooms = event.rooms.map { it.id }.toSet() - this.rooms.keys.toSet()
                    val removedRooms = this.rooms.keys.toSet() - event.rooms.map { it.id }.toSet()

                    val changedRooms = this.rooms.values.mapNotNull { existing ->
                        event.rooms.find { it.id == existing.id }?.let { new ->
                            existing to new
                        }
                    }.filter { (existing, new) ->
                        new != existing
                    }.map { (existing, _) ->
                        existing.id
                    }

                    val changedReadStates = this.unreadCounts.mapNotNull { (roomId, existing) ->
                        event.readStates.find { it.roomId == roomId }?.let { newReadState ->
                            Triple(roomId, existing, newReadState.unreadCount)
                        }
                    }.filter { (_, existing, new) ->
                        new != existing
                    }.map { (roomId, _, _) ->
                        roomId
                    }

                    val changedMembers = this.members.mapNotNull { (roomId, existing) ->
                        event.memberships.find { it.roomId == roomId }?.let { newMemberships ->
                            Triple(roomId, existing, newMemberships.userIds.toSet())
                        }
                    }.filter { (_, existing, new) ->
                        new != existing
                    }

                    this.initialiseContents(event.rooms, event.memberships, event.readStates)

                    val addedEvents = addedRooms.map { UserInternalEvent.AddedToRoom(this[it]!!) }
                    val removedEvents = removedRooms.map { UserInternalEvent.RemovedFromRoom(it) }
                    val updatedEvents = (changedRooms + changedReadStates).toSet().map {
                        UserInternalEvent.RoomUpdated(this[it]!!)
                    }
                    val membershipEvents = changedMembers.flatMap { (roomId, existing, new) ->
                        (new - existing).map { UserInternalEvent.UserJoinedRoom(it, roomId) } +
                        (existing - new).map { UserInternalEvent.UserLeftRoom(it, roomId) }
                    }

                    addedEvents + removedEvents + updatedEvents + membershipEvents
                }
                is UserSubscriptionEvent.AddedToRoomEvent -> {
                    this.rooms[event.room.id] = event.room
                    this.members[event.memberships.roomId] = event.memberships.userIds.toSet()
                    this.unreadCounts[event.readState.roomId] = event.readState.unreadCount

                    listOf(UserInternalEvent.AddedToRoom(this[event.room.id]!!))
                }
                is UserSubscriptionEvent.RoomUpdatedEvent -> {
                    this.rooms[event.room.id] = event.room
                    listOf(UserInternalEvent.RoomUpdated(this[event.room.id]!!))
                }
                is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                    this.unreadCounts[event.readState.roomId] = event.readState.unreadCount
                    listOf(UserInternalEvent.RoomUpdated(this[event.readState.roomId]!!))
                }
                is UserSubscriptionEvent.RoomDeletedEvent -> {
                    this.remove(event.roomId)
                    listOf(UserInternalEvent.RoomDeleted(event.roomId))
                }
                is UserSubscriptionEvent.RemovedFromRoomEvent -> {
                    this.remove(event.roomId)
                    listOf(UserInternalEvent.RemovedFromRoom(event.roomId))
                }
                is UserSubscriptionEvent.UserJoinedRoomEvent -> {
                    this.members[event.roomId]?.let { existingMembers ->
                        this.members[event.roomId] =
                                existingMembers + event.userId
                    }
                    listOf(UserInternalEvent.UserJoinedRoom(event.userId, event.roomId))
                }
                is UserSubscriptionEvent.UserLeftRoomEvent -> {
                    this.members[event.roomId]?.let { existingMembers ->
                        this.members[event.roomId] =
                                existingMembers - event.userId
                    }
                    listOf(UserInternalEvent.UserLeftRoom(event.userId, event.roomId))
                }
                is UserSubscriptionEvent.ErrorOccurred ->
                    listOf(UserInternalEvent.ErrorOccurred(event.error))
            }
}

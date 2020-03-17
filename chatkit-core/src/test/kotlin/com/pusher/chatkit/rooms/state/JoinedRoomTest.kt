package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatState
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.State
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomTest : Spek({

    describe("given no rooms") {
        val givenState = State.initial().with(
            ChatState.initial().with(
                joinedRoomsState = JoinedRoomsState(
                    rooms = emptyMap(),
                    unreadCounts = emptyMap()
                )
            )
        )

        describe("when a room is joined") {
            val joinedRoom = JoinedRoom(roomOne, unreadCount = 1)
            val newState = joinedRoomReducer(givenState, joinedRoom)

            it("then the state contains the expected room") {
                assertThat(newState.chatState.joinedRoomsState)
                    .isNotNull()
                    .containsOnly(roomOneId to roomOne)
            }

            it("then the state contains the expected unread count") {
                assertThat(newState.chatState.joinedRoomsState)
                    .isNotNull()
                    .containsOnlyUnreadCounts(roomOneId to 1)
            }
        }
    }

    describe("given one room") {
        val givenState = State.initial().with(
            ChatState.initial().with(
                joinedRoomsState = JoinedRoomsState(
                    rooms = mapOf(roomOneId to roomOne),
                    unreadCounts = mapOf(roomOneId to 1)
                )
            )
        )

        describe("when a room is joined") {
            val joinedRoom = JoinedRoom(roomTwo, unreadCount = 2)
            val newState = joinedRoomReducer(givenState, joinedRoom)

            it("then the state contains the expected room") {
                assertThat(newState.chatState.joinedRoomsState)
                    .isNotNull()
                    .containsOnly(
                        roomOneId to roomOne,
                        roomTwoId to roomTwo
                    )
            }

            it("then the state contains the expected unread count") {
                assertThat(newState.chatState.joinedRoomsState)
                    .isNotNull()
                    .containsOnlyUnreadCounts(
                        roomOneId to 1,
                        roomTwoId to 2
                    )
            }
        }
    }
})

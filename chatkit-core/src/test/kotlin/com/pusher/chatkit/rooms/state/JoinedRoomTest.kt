package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.JoinedRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomTest : Spek({

    describe("given no rooms") {
        val givenState = ChatkitState(joinedRoomsState = JoinedRoomsState(emptyMap(), emptyMap()))

        describe("when one new room with unread counts is received") {
            val joinedRoom = JoinedRoom(JoinedRoomsStateTestUtil.roomOne, unreadCount = 1)
            val newState = joinedRoomReducer(givenState, joinedRoom)

            it("then the state should contain the expected rooms") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(
                        JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne
                )
            }

            it("then the state should contain the expected unread count") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 1
                )
            }
        }
    }
})

package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.UpdatedRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomUpdatedTest : Spek({

    describe("given a joined room state with one room") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne),
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to 1)))

        describe("when an event for updating a room that is part of the state is received") {
            val newState = roomUpdatedReducer(initialState,
                    UpdatedRoom(JoinedRoomsStateTestUtil.roomOneUpdated))

            it("then the state should contain the updated room") {
                assertThat(newState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomOneId
                                to JoinedRoomsStateTestUtil.roomOneUpdated)
            }
        }
    }
})
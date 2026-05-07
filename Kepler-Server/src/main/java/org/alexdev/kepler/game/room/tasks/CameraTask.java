package org.alexdev.kepler.game.room.tasks;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.room.entities.RoomEntity;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.messages.outgoing.rooms.user.USER_STATUSES;

import java.util.List;

public class CameraTask implements Runnable {
    private final Entity entity;

    public CameraTask(Entity entity) {
        this.entity = entity;
    }

    @Override
    public void run() {
        if (this.entity == null) {
            return;
        }

        RoomEntity roomUser = this.entity.getRoomUser();

        if (roomUser == null || roomUser.getRoom() == null) {
            return;
        }

        // The user can leave the camera animation early (walk, sit, log out)
        // and clear USE_ITEM before this scheduled task fires. Without this
        // guard, getStatus(USE_ITEM).getValue() NPEs and aborts the worker
        // thread that owns the GameScheduler tick.
        if (!roomUser.containsStatus(StatusType.USE_ITEM)) {
            return;
        }

        String item = roomUser.getStatus(StatusType.USE_ITEM).getValue();

        roomUser.removeStatus(StatusType.USE_ITEM);
        roomUser.setStatus(StatusType.CARRY_ITEM, item);

        if (!roomUser.isWalking()) {
            roomUser.getRoom().send(new USER_STATUSES(List.of(this.entity)));
        }
    }
}

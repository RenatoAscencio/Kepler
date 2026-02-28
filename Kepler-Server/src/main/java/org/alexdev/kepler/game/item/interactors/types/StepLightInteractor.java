package org.alexdev.kepler.game.item.interactors.types;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.entities.RoomEntity;
import org.alexdev.kepler.game.triggers.GenericTrigger;

public class StepLightInteractor extends GenericTrigger {

    @Override
    public void onEntityStep(Entity entity, RoomEntity roomEntity, Item item, Position oldPosition) {
        // Toggle light on when stepped on
        if (item.getCustomData().equals("0") || item.getCustomData().isEmpty()) {
            item.setCustomData("1");
        } else {
            item.setCustomData("0");
        }
        item.updateStatus();
    }

    @Override
    public void onEntityLeave(Entity entity, RoomEntity roomEntity, Item item) {
        // Keep light state when leaving
    }
}

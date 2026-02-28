package org.alexdev.kepler.game.item.interactors.types;

import org.alexdev.kepler.game.GameScheduler;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.entities.RoomEntity;
import org.alexdev.kepler.game.triggers.GenericTrigger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LoveRandomizerInteractor extends GenericTrigger {

    public void onInteract(Player player, Room room, Item item, int status) {
        // Start randomizer animation
        item.setCustomData("-1");
        item.updateStatus();

        // After 2 seconds, show random result (1-10)
        GameScheduler.getInstance().getService().schedule(() -> {
            int result = ThreadLocalRandom.current().nextInt(1, 11);
            item.setCustomData(String.valueOf(result));
            item.updateStatus();
        }, 2, TimeUnit.SECONDS);
    }
}

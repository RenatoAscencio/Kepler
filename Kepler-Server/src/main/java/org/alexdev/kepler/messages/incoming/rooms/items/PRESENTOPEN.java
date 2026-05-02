package org.alexdev.kepler.messages.incoming.rooms.items;

import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.base.ItemDefinition;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.outgoing.catalogue.DELIVER_PRESENT;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class PRESENTOPEN implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return;
        }

        if (!room.isOwner(player.getDetails().getId()) && !player.hasFuse(Fuseright.ANY_ROOM_CONTROLLER)) {
            return;
        }

        String contents = reader.contents();

        if (!StringUtils.isNumeric(contents)) {
            Log.getErrorLogger().warn("Ignored present open request with invalid item id '{}'", contents);
            return;
        }

        int itemId = Integer.parseInt(contents);
        Item item = room.getItemManager().getById(itemId);

        if (item == null || !item.hasBehaviour(ItemBehaviour.PRESENT)) {
            return;
        }

        String[] presentData = item.getCustomData().split(Pattern.quote(Item.PRESENT_DELIMETER), -1);

        if (presentData.length < 5) {
            Log.getErrorLogger().error("Could not open malformed present item {} with data '{}'", item.getId(), item.getCustomData());
            return;
        }

        String saleCode = presentData[0];
        String receivedFrom = presentData[1];
        String extraData = presentData[3];
        long timestamp = parsePresentTimestamp(presentData[4]);

        CatalogueItem catalogueItem = getCatalogueItem(saleCode);

        if (catalogueItem == null) {
            Log.getErrorLogger().error("Could not open present item {} because catalogue item '{}' was not found", item.getId(), saleCode);
            return;
        }

        ItemDefinition definition = catalogueItem.getDefinition();

        if (!catalogueItem.isPackage() && definition == null) {
            Log.getErrorLogger().error("Could not open present item {} because catalogue item '{}' has no item definition", item.getId(), saleCode);
            return;
        }

        // Don't create a new item instance, reuse if the item isn't a trophy or teleporter, etc
        if (!catalogueItem.isPackage() && !definition.hasBehaviour(ItemBehaviour.PRIZE_TROPHY) &&
                !definition.hasBehaviour(ItemBehaviour.TELEPORTER) &&
                !definition.hasBehaviour(ItemBehaviour.ROOMDIMMER) &&
                !definition.hasBehaviour(ItemBehaviour.DECORATION) &&
                !definition.hasBehaviour(ItemBehaviour.POST_IT) &&
                !definition.getSprite().equalsIgnoreCase("film")) {
            room.getMapping().removeItem(item);

            item.setDefinitionId(definition.getId());
            item.setCustomData(extraData);
            item.save();

            player.send(new DELIVER_PRESENT(definition.getSprite(), extraData, definition.getColour()));

            player.getInventory().addItem(item);
            player.getInventory().getView("new");
        } else {
            List<Item> itemList = CatalogueManager.getInstance().purchase(player, catalogueItem, extraData, receivedFrom, timestamp);
            boolean removePresent = false;

            if (!itemList.isEmpty()) {
                var giftedItem = itemList.get(0);

                player.send(new DELIVER_PRESENT(giftedItem.getDefinition().getSprite(), extraData, giftedItem.getDefinition().getColour()));
                player.getInventory().getView("new");
                removePresent = true;
            } else {
                // itemList will be blank if this was film purchased, however, still show film when gift is opened
                if (definition != null && definition.getSprite().equalsIgnoreCase("film")) {
                    player.send(new DELIVER_PRESENT("film", null, null));
                    removePresent = true;
                }
            }

            if (!removePresent) {
                Log.getErrorLogger().error("Could not open present item {} because catalogue item '{}' did not deliver any item", item.getId(), saleCode);
                return;
            }

            room.getMapping().removeItem(item);
            item.delete();
        }

    }

    private CatalogueItem getCatalogueItem(String saleCode) {
        if (StringUtils.isNumeric(saleCode)) {
            int catalogueId = Integer.parseInt(saleCode);
            return CatalogueManager.getInstance().getCatalogueItems().stream()
                    .filter(shopItem -> shopItem.getId() == catalogueId)
                    .findFirst()
                    .orElse(null);
        }

        return CatalogueManager.getInstance().getCatalogueItem(saleCode, true);
    }

    private long parsePresentTimestamp(String value) {
        if (!StringUtils.isNumeric(value)) {
            Log.getErrorLogger().warn("Invalid present timestamp '{}', using current timestamp", value);
            return System.currentTimeMillis() / 1000;
        }

        return Long.parseLong(value);
    }
}

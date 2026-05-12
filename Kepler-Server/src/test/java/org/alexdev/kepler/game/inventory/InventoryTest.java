package org.alexdev.kepler.game.inventory;

import io.netty.buffer.Unpooled;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemDefinition;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryTest {

    @Test
    void serialisesGateWithoutStateInHandStrip() throws ReflectiveOperationException {
        assertGateInventoryStrip("country_gate", 2, "C");
    }

    @Test
    void serialisesExecutiveGateWithoutStateInHandStrip() throws ReflectiveOperationException {
        assertGateInventoryStrip("exe_gate", 1, "C");
    }

    private static void assertGateInventoryStrip(String sprite, int length, String customData) throws ReflectiveOperationException {
        Item item = new Item();
        setField(item, "id", 66);
        setField(item, "customData", customData);
        setField(item, "definition", new ItemDefinition(
            820,
            sprite,
            "Farm Gate",
            "Livestock: Close gate behind you",
            "solid,requires_rights_for_interaction,gate",
            "default",
            0,
            length,
            1,
            "0,0,0",
            "",
            true,
            true
        ));

        NettyResponse response = new NettyResponse((short) 140, Unpooled.buffer());
        Inventory.serialise(response, item, 0);

        String separator = Character.toString((char) 30);
        String body = response.getBodyString();

        assertThat(body).contains("SI" + separator + "66" + separator + "0" + separator + "S" + separator);
        assertThat(body).contains(sprite + separator + length + separator + "1" + separator + separator + "0,0,0");
        assertThat(body).doesNotContain(separator + customData + separator);
    }

    private static void setField(Item item, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Item.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(item, value);
    }
}

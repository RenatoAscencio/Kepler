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
        Item item = new Item();
        setField(item, "id", 66);
        setField(item, "customData", "C");
        setField(item, "definition", new ItemDefinition(
            738,
            "exe_gate",
            "Executive Gate",
            "Keeps the tax man away",
            "solid,requires_rights_for_interaction,gate",
            "default",
            0,
            1,
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
        assertThat(body).contains("exe_gate" + separator + "1" + separator + "1" + separator + separator + "0,0,0");
        assertThat(body).doesNotContain(separator + "C" + separator);
    }

    private static void setField(Item item, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Item.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(item, value);
    }
}

package org.alexdev.kepler.game.item.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemDefinitionTest {

    @Test
    void normalisesBlankGateCustomDataToClosed() {
        ItemDefinition gate = new ItemDefinition(
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
        );

        assertThat(gate.normaliseCustomData(null)).isEqualTo("C");
        assertThat(gate.normaliseCustomData("")).isEqualTo("C");
        assertThat(gate.normaliseCustomData("   ")).isEqualTo("C");
    }

    @Test
    void keepsExistingGateState() {
        ItemDefinition gate = new ItemDefinition(
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
        );

        assertThat(gate.normaliseCustomData("O")).isEqualTo("O");
        assertThat(gate.normaliseCustomData("C")).isEqualTo("C");
    }

    @Test
    void hidesGateStateFromInventoryCustomData() {
        ItemDefinition gate = new ItemDefinition(
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
        );

        assertThat(gate.getInventoryCustomData(null)).isEqualTo("");
        assertThat(gate.getInventoryCustomData("C")).isEqualTo("");
        assertThat(gate.getInventoryCustomData("O")).isEqualTo("");
    }

    @Test
    void leavesBlankCustomDataForNonGateItems() {
        ItemDefinition desk = new ItemDefinition(
            735,
            "exe_table",
            "Executive Desk",
            "Take a memo, Featherstone",
            "solid,custom_data_numeric_state,requires_rights_for_interaction",
            "default",
            0,
            3,
            2,
            "0,0,0",
            "",
            true,
            true
        );

        assertThat(desk.normaliseCustomData("")).isEqualTo("");
        assertThat(desk.getInventoryCustomData("2")).isEqualTo("2");
    }
}

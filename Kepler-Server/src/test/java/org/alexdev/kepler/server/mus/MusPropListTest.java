package org.alexdev.kepler.server.mus;

import org.alexdev.kepler.server.mus.streams.MusPropList;
import org.alexdev.kepler.server.mus.streams.MusTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MusPropListTest {

    @Test
    void newPropListIsEmpty() {
        MusPropList props = new MusPropList(3);

        assertThat(props.length()).isEqualTo(3);
        assertThat(props.getSymbolAt(0)).isNull();
        assertThat(props.getDataAt(0)).isEmpty();
    }

    @Test
    void setPropAsBytesFillsFirstAvailableSlot() {
        MusPropList props = new MusPropList(2);

        boolean stored = props.setPropAsBytes("image", MusTypes.Media, new byte[] { 0x01 });

        assertThat(stored).isTrue();
        assertThat(props.getSymbolAt(0)).isEqualTo("image");
        assertThat(props.getDataTypeAt(0)).isEqualTo(MusTypes.Media);
        assertThat(props.getDataAt(0)).containsExactly(0x01);
    }

    @Test
    void setPropAsBytesReturnsFalseWhenFull() {
        MusPropList props = new MusPropList(1);
        props.setPropAsBytes("a", MusTypes.Integer, new byte[] { 0 });

        boolean stored = props.setPropAsBytes("b", MusTypes.Integer, new byte[] { 0 });

        assertThat(stored).isFalse();
    }

    @Test
    void getPropAsIntReturnsMinusOneWhenAbsent() {
        MusPropList props = new MusPropList(1);

        assertThat(props.getPropAsInt("missing")).isEqualTo(-1);
    }

    @Test
    void getPropAsBytesReturnsEmptyArrayWhenAbsent() {
        MusPropList props = new MusPropList(1);

        assertThat(props.getPropAsBytes("missing")).isEmpty();
    }

    @Test
    void setPropAsIntStoresFourByteBigEndianRepresentation() {
        MusPropList props = new MusPropList(1);
        props.setPropAsInt("cs", 0x12345678);

        byte[] raw = props.getPropAsBytes("cs");

        assertThat(raw).hasSize(4);
        assertThat(props.getPropAsInt("cs")).isEqualTo(0x12345678);
    }

    @Test
    void setPropAsStringRetrievableByKey() {
        MusPropList props = new MusPropList(1);
        props.setPropAsString("time", "2026-05-07");

        assertThat(props.getPropAsString("time")).isEqualTo("2026-05-07");
    }

    /**
     * Photo text comes from Spanish users (Mi habitación, etc.); bytes used by the
     * MUS encoder must round-trip without locale-dependent corruption. This pins
     * the contract that the prop list stores raw bytes faithfully regardless of
     * default JVM charset.
     */
    @Test
    void setPropAsStringPreservesSpanishCharacters() {
        MusPropList props = new MusPropList(1);
        String spanish = "Mi habitación";

        props.setPropAsString("note", spanish);

        assertThat(props.getPropAsString("note")).isEqualTo(spanish);
    }
}

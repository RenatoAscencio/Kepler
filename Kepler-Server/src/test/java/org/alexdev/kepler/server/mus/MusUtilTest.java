package org.alexdev.kepler.server.mus;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.alexdev.kepler.server.mus.streams.MusPropList;
import org.alexdev.kepler.server.mus.streams.MusTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusUtilTest {

    @Test
    void writesAndReadsAsciiStringRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        MusUtil.writeEvenPaddedString(buf, "BINDATA");

        String result = MusUtil.readEvenPaddedString(buf);

        assertThat(result).isEqualTo("BINDATA");
        assertThat(buf.readableBytes()).isZero();
    }

    @Test
    void writesAndReadsEmptyStringRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        MusUtil.writeEvenPaddedString(buf, "");

        String result = MusUtil.readEvenPaddedString(buf);

        assertThat(result).isEmpty();
    }

    @Test
    void evenLengthAsciiHasNoPaddingByte() {
        ByteBuf buf = Unpooled.buffer();
        MusUtil.writeEvenPaddedString(buf, "ABCD");

        // 4 bytes length prefix + 4 bytes data + 0 padding = 8
        assertThat(buf.readableBytes()).isEqualTo(8);
    }

    @Test
    void oddLengthAsciiHasPaddingByte() {
        ByteBuf buf = Unpooled.buffer();
        MusUtil.writeEvenPaddedString(buf, "ABC");

        // 4 bytes length prefix + 3 bytes data + 1 padding = 8
        assertThat(buf.readableBytes()).isEqualTo(8);
    }

    /**
     * Regression test for length-prefix bug: write must encode BYTE count, not char count.
     * Spanish photo text uses multi-byte UTF-8 chars (ñ, é, í, ó, ú).
     * If writer emits char count and reader interprets as byte count, the round-trip fails.
     */
    @Test
    void roundTripPreservesUtf8MultiByteCharacters() {
        ByteBuf buf = Unpooled.buffer();
        String spanishText = "habitación";

        MusUtil.writeEvenPaddedString(buf, spanishText);
        String result = MusUtil.readEvenPaddedString(buf);

        assertThat(result).isEqualTo(spanishText);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mi habitación",
            "Soy un usuario nuevo!",
            "Viviendo en HabboP",
            "café",
            "niño",
            "üñîçødé"
    })
    void roundTripSurvivesNonAsciiSamples(String original) {
        ByteBuf buf = Unpooled.buffer();
        MusUtil.writeEvenPaddedString(buf, original);

        String result = MusUtil.readEvenPaddedString(buf);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void rejectsStringLengthAboveLimit() {
        ByteBuf buf = Unpooled.buffer();
        // Write a fake length prefix > MAX_STRING_LENGTH (64KB)
        buf.writeInt(64 * 1024 + 1);

        assertThatThrownBy(() -> MusUtil.readEvenPaddedString(buf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid MUS string length");
    }

    @Test
    void rejectsNegativeStringLength() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(-1);

        assertThatThrownBy(() -> MusUtil.readEvenPaddedString(buf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid MUS string length");
    }

    @Test
    void rejectsTruncatedStringPayload() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(10); // claim 10 bytes
        buf.writeBytes("ABC".getBytes()); // only 3 available

        assertThatThrownBy(() -> MusUtil.readEvenPaddedString(buf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds readable bytes");
    }

    @Test
    void writesAndReadsPropListWithBytesAndIntsAndStrings() {
        ByteBuf buf = Unpooled.buffer();

        MusPropList original = new MusPropList(3);
        original.setPropAsBytes("image", MusTypes.Media, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
        original.setPropAsString("time", "2026-05-07 13:00:00");
        original.setPropAsInt("cs", 0xCAFEBABE);

        MusUtil.writePropList(buf, original);
        MusPropList parsed = MusUtil.readPropList(buf);

        assertThat(parsed.length()).isEqualTo(3);
        assertThat(parsed.getPropAsBytes("image")).containsExactly(0x01, 0x02, 0x03, 0x04, 0x05);
        assertThat(parsed.getPropAsString("time")).isEqualTo("2026-05-07 13:00:00");
        assertThat(parsed.getPropAsInt("cs")).isEqualTo(0xCAFEBABE);
    }

    @Test
    void propListPreservesLargeMediaPayload() {
        ByteBuf buf = Unpooled.buffer();

        // Simulate a 4MB camera photo payload — the dc4543c fix raised the limit
        // to 32MB so 4MB must round-trip cleanly.
        byte[] largeImage = new byte[4 * 1024 * 1024];
        for (int i = 0; i < largeImage.length; i++) {
            largeImage[i] = (byte) (i & 0xFF);
        }

        MusPropList props = new MusPropList(1);
        props.setPropAsBytes("image", MusTypes.Media, largeImage);

        MusUtil.writePropList(buf, props);
        MusPropList parsed = MusUtil.readPropList(buf);

        assertThat(parsed.getPropAsBytes("image")).hasSize(largeImage.length);
        assertThat(parsed.getPropAsBytes("image")).isEqualTo(largeImage);
    }

    @Test
    void rejectsPropDataLengthAboveLimit() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1); // list length = 1
        buf.writeShort(MusTypes.Symbol);
        MusUtil.writeEvenPaddedString(buf, "image");
        buf.writeShort(MusTypes.Media);
        buf.writeInt(33 * 1024 * 1024); // 33MB exceeds the 32MB cap

        assertThatThrownBy(() -> MusUtil.readPropList(buf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid MUS prop data length");
    }
}

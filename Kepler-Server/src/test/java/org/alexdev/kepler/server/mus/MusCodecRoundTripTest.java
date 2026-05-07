package org.alexdev.kepler.server.mus;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.alexdev.kepler.server.mus.codec.MusNetworkDecoder;
import org.alexdev.kepler.server.mus.codec.MusNetworkEncoder;
import org.alexdev.kepler.server.mus.streams.MusMessage;
import org.alexdev.kepler.server.mus.streams.MusPropList;
import org.alexdev.kepler.server.mus.streams.MusTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the encoder + frame decoder + message decoder pipeline as it would run
 * on the wire, verifying that BINDATA-style PropList messages with binary photo
 * payloads survive a full round trip.
 */
class MusCodecRoundTripTest {
    private static final int MUS_HEADER_LENGTH = 6;
    private static final int MAX_MUS_MESSAGE_SIZE = 32 * 1024 * 1024;
    private static final int MAX_MUS_FRAME_SIZE = MAX_MUS_MESSAGE_SIZE + MUS_HEADER_LENGTH;

    @Test
    void stringMessageRoundTripsThroughFullPipeline() {
        MusMessage original = new MusMessage();
        original.setSubject("BINDATA_SAVED");
        original.setContentType(MusTypes.String);
        original.setContentString("42");

        MusMessage decoded = roundTrip(original);

        assertThat(decoded.getSubject()).isEqualTo("BINDATA_SAVED");
        assertThat(decoded.getContentType()).isEqualTo(MusTypes.String);
        assertThat(decoded.getContentString()).isEqualTo("42");
    }

    @Test
    void propListMessageWithMediaRoundTripsThroughFullPipeline() {
        byte[] photo = new byte[64 * 1024];
        for (int i = 0; i < photo.length; i++) {
            photo[i] = (byte) (i & 0xFF);
        }

        MusPropList props = new MusPropList(3);
        props.setPropAsBytes("image", MusTypes.Media, photo);
        props.setPropAsString("time", "2026-05-07 13:00:00");
        props.setPropAsInt("cs", 0xCAFEBABE);

        MusMessage original = new MusMessage();
        original.setSubject("BINARYDATA");
        original.setContentType(MusTypes.PropList);
        original.setContentPropList(props);

        MusMessage decoded = roundTrip(original);

        assertThat(decoded.getSubject()).isEqualTo("BINARYDATA");
        assertThat(decoded.getContentType()).isEqualTo(MusTypes.PropList);
        assertThat(decoded.getContentPropList().getPropAsBytes("image")).isEqualTo(photo);
        assertThat(decoded.getContentPropList().getPropAsInt("cs")).isEqualTo(0xCAFEBABE);
    }

    @Test
    void multiMegabytePhotoSurvivesRoundTrip() {
        byte[] photo = new byte[2 * 1024 * 1024]; // 2MB — well above legacy 1MB cap
        for (int i = 0; i < photo.length; i++) {
            photo[i] = (byte) ((i * 31) & 0xFF);
        }

        MusPropList props = new MusPropList(1);
        props.setPropAsBytes("image", MusTypes.Media, photo);

        MusMessage original = new MusMessage();
        original.setSubject("BINARYDATA");
        original.setContentType(MusTypes.PropList);
        original.setContentPropList(props);

        MusMessage decoded = roundTrip(original);

        assertThat(decoded.getContentPropList().getPropAsBytes("image")).isEqualTo(photo);
    }

    private MusMessage roundTrip(MusMessage message) {
        EmbeddedChannel encoderChannel = new EmbeddedChannel(new MusNetworkEncoder());
        encoderChannel.writeOutbound(message);

        EmbeddedChannel decoderChannel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(MAX_MUS_FRAME_SIZE, 2, 4, 0, 0),
                new MusNetworkDecoder());

        Object outbound;
        while ((outbound = encoderChannel.readOutbound()) != null) {
            decoderChannel.writeInbound(outbound);
        }

        MusMessage decoded = decoderChannel.readInbound();
        decoderChannel.finishAndReleaseAll();
        encoderChannel.finishAndReleaseAll();

        return decoded;
    }
}

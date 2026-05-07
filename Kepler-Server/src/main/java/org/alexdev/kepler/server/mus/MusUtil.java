package org.alexdev.kepler.server.mus;

import io.netty.buffer.ByteBuf;
import org.alexdev.kepler.server.mus.streams.MusPropList;
import org.alexdev.kepler.server.mus.streams.MusTypes;

public class MusUtil {
    private static final int MAX_STRING_LENGTH = 64 * 1024;
    private static final int MAX_PROP_LIST_LENGTH = 128;
    private static final int MAX_PROP_DATA_LENGTH = 32 * 1024 * 1024;

    public static String readEvenPaddedString(ByteBuf in) {
        // String length
        int length = in.readInt();

        if (length < 0 || length > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("Invalid MUS string length: " + length);
        }

        if (length == 0) {
            return "";
        }

        int padding = (length % 2) != 0 ? 1 : 0;

        if (in.readableBytes() < length + padding) {
            throw new IllegalArgumentException("MUS string length exceeds readable bytes: " + length);
        }

        // Actual string bytes
        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        // Advance one byte if uneven
        if (padding == 1) {
            in.readByte();
        }

        // Return the string
        return new String(bytes);
    }

    public static void writeEvenPaddedString(ByteBuf out, String str) {
        // String length
        out.writeInt(str.length());

        // Actual string bytes
        out.writeBytes(str.getBytes());

        // Add a null byte if uneven
        if ((str.length() % 2) != 0) {
            out.writeByte(0);
        }
    }

    public static MusPropList readPropList(ByteBuf in) {
        // Length of list
        int length = in.readInt();

        if (length < 0 || length > MAX_PROP_LIST_LENGTH) {
            throw new IllegalArgumentException("Invalid MUS prop list length: " + length);
        }

        // Allocate props
        MusPropList props = new MusPropList(length);

        // Parse them
        for (int i = 0; i < length; i++) {
            // Symbol type (always string)
            in.readShort();

            // Symbol (key)
            String symbol = MusUtil.readEvenPaddedString(in);

            // Data type
            short dataType = in.readShort();

            // Data (value)
            int dataLength;
            if (dataType == MusTypes.Integer) {
                dataLength = 4;
            } else {
                dataLength = in.readInt();
            }

            if (dataLength < 0 || dataLength > MAX_PROP_DATA_LENGTH) {
                throw new IllegalArgumentException("Invalid MUS prop data length: " + dataLength);
            }

            int padding = (dataLength % 2) != 0 ? 1 : 0;

            if (in.readableBytes() < dataLength + padding) {
                throw new IllegalArgumentException("MUS prop data length exceeds readable bytes: " + dataLength);
            }

            byte[] data = new byte[dataLength];
            in.readBytes(data);
            if (padding == 1) {
                in.readByte();
            }

            // Set prop
            props.setPropAsBytes(symbol, dataType, data);
        }

        return props;
    }

    public static void writePropList(ByteBuf out, MusPropList props) {
        // Length
        out.writeInt(props.length());

        // Serialize elements
        for (int i = 0; i < props.length(); i++) {
            // Symbol
            out.writeShort(MusTypes.Symbol);
            MusUtil.writeEvenPaddedString(out, props.getSymbolAt(i));

            // Value
            out.writeShort(props.getDataTypeAt(i));
            byte[] data = props.getDataAt(i);
            if (props.getDataTypeAt(i) != MusTypes.Integer) {
                out.writeInt(data.length);
            }
            out.writeBytes(data);
            if ((data.length % 2) != 0) {
                out.writeByte(0);
            }
        }
    }
}

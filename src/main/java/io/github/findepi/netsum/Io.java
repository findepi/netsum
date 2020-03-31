/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.findepi.netsum;

import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.readFully;

final class Io
{
    private Io() {}

    public static void writePacket(OutputStream outputStream, byte[] workBuffer, byte[] data, int offset, int length)
            throws IOException
    {
        checkArgument(offset >= 0 && offset + length <= data.length, "Invalid range: %s, %s", offset, length);
        encodeInt(length, workBuffer, 0);
        writeFully(outputStream, workBuffer, 0, 4);
        writeFully(outputStream, data, offset, length);
    }

    public static int readPacket(InputStream inputStream, byte[] buffer)
            throws IOException
    {
        readFully(inputStream, buffer, 0, 4);
        int length = decodeInt(buffer, 0);
        verify(length >= 0, "negative length: %s", length);
        readFully(inputStream, buffer, 0, length);
        return length;
    }

    @VisibleForTesting
    @SuppressWarnings({"NumericCastThatLosesPrecision", "PointlessArithmeticExpression"})
    static void encodeInt(int value, byte[] bytes, int offset)
    {
        bytes[offset + 0] = (byte) ((value >> (8 * 3)) & 0xff);
        bytes[offset + 1] = (byte) ((value >> (8 * 2)) & 0xff);
        bytes[offset + 2] = (byte) ((value >> (8 * 1)) & 0xff);
        bytes[offset + 3] = (byte) ((value >> (8 * 0)) & 0xff);
    }

    @VisibleForTesting
    @SuppressWarnings("PointlessArithmeticExpression")
    static int decodeInt(byte[] bytes, int offset)
    {
        return (bytes[offset] & 0xff) << (8 * 3) |
                (bytes[offset + 1] & 0xff) << (8 * 2) |
                (bytes[offset + 2] & 0xff) << (8 * 1) |
                (bytes[offset + 3] & 0xff) << (8 * 0);
    }

    @VisibleForTesting
    @SuppressWarnings({"NumericCastThatLosesPrecision", "PointlessArithmeticExpression"})
    static void encodeLong(long value, byte[] bytes, int offset)
    {
        bytes[offset + 0] = (byte) ((value >> (8 * 7)) & 0xff);
        bytes[offset + 1] = (byte) ((value >> (8 * 6)) & 0xff);
        bytes[offset + 2] = (byte) ((value >> (8 * 5)) & 0xff);
        bytes[offset + 3] = (byte) ((value >> (8 * 4)) & 0xff);
        bytes[offset + 4] = (byte) ((value >> (8 * 3)) & 0xff);
        bytes[offset + 5] = (byte) ((value >> (8 * 2)) & 0xff);
        bytes[offset + 6] = (byte) ((value >> (8 * 1)) & 0xff);
        bytes[offset + 7] = (byte) ((value >> (8 * 0)) & 0xff);
    }

    @VisibleForTesting
    @SuppressWarnings("PointlessArithmeticExpression")
    static long decodeLong(byte[] bytes, int offset)
    {
        return ((long) bytes[offset] & 0xff) << (8 * 7) |
                ((long) bytes[offset + 1] & 0xff) << (8 * 6) |
                ((long) bytes[offset + 2] & 0xff) << (8 * 5) |
                ((long) bytes[offset + 3] & 0xff) << (8 * 4) |
                ((long) bytes[offset + 4] & 0xff) << (8 * 3) |
                ((long) bytes[offset + 5] & 0xff) << (8 * 2) |
                ((long) bytes[offset + 6] & 0xff) << (8 * 1) |
                ((long) bytes[offset + 7] & 0xff) << (8 * 0);
    }

    public static void writeFully(OutputStream outputStream, byte[] bytes, int offset, int length)
            throws IOException
    {
        copy(new ByteArrayInputStream(bytes, offset, length), outputStream);
    }
}

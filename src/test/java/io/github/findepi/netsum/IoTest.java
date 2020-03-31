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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static io.github.findepi.netsum.Io.decodeInt;
import static io.github.findepi.netsum.Io.decodeLong;
import static io.github.findepi.netsum.Io.encodeInt;
import static io.github.findepi.netsum.Io.encodeLong;
import static io.github.findepi.netsum.Io.readPacket;
import static io.github.findepi.netsum.Io.writePacket;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IoTest
{
    @Test
    public void testIntEncoding()
    {
        testIntEncoding(Integer.MIN_VALUE);
        testIntEncoding(-1_234_456_890);
        testIntEncoding(-10_042);
        testIntEncoding(-42);
        testIntEncoding(0);
        testIntEncoding(42);
        testIntEncoding(10_042);
        testIntEncoding(1_234_456_890);
        testIntEncoding(Integer.MAX_VALUE);
    }

    private void testIntEncoding(int value)
    {
        byte[] buffer = new byte[4];
        encodeInt(value, buffer, 0);
        assertEquals(value, decodeInt(buffer, 0));

        buffer = new byte[17];
        encodeInt(value, buffer, 3);
        assertEquals(value, decodeInt(buffer, 3));
    }

    @Test
    public void testLongEncoding()
    {
        testLongEncoding(Long.MIN_VALUE);
        testLongEncoding(-8_123_456_789_012_345_678L);
        testLongEncoding(-1_234_456_890);
        testLongEncoding(-10_042);
        testLongEncoding(-42);
        testLongEncoding(0);
        testLongEncoding(42);
        testLongEncoding(10_042);
        testLongEncoding(1_234_456_890);
        testLongEncoding(8_123_456_789_012_345_678L);
        testLongEncoding(Long.MAX_VALUE);
    }

    private void testLongEncoding(long value)
    {
        byte[] buffer = new byte[8];
        encodeLong(value, buffer, 0);
        assertEquals(value, decodeLong(buffer, 0));

        buffer = new byte[17];
        encodeLong(value, buffer, 3);
        assertEquals(value, decodeLong(buffer, 3));
    }

    @Test
    @SuppressWarnings("resource")
    public void testPacket()
            throws Exception
    {
        byte[] workBuffer = new byte[13];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writePacket(outputStream, workBuffer, new byte[] {1, 2, 3}, 1, 0);
        writePacket(outputStream, workBuffer, "xABC".getBytes(UTF_8), 1, 3);
        writePacket(outputStream, workBuffer, new byte[] {0, 42, 127, -125}, 0, 4);
        outputStream.write("garbage".getBytes(UTF_8));
        byte[] bytes = outputStream.toByteArray();

        byte[] buffer = new byte[1024];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        assertEquals(0, readPacket(inputStream, buffer));

        assertEquals(3, readPacket(inputStream, buffer));
        assertEquals("ABC", new String(buffer, 0, 3, UTF_8));

        assertEquals(4, readPacket(inputStream, buffer));
        assertArrayEquals(new byte[] {0, 42, 127, -125}, copyOfRange(buffer, 0, 4));
    }
}

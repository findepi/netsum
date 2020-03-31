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

import java.util.function.IntFunction;

import static io.github.findepi.netsum.Packets.createPacket;
import static io.github.findepi.netsum.Packets.verifyPacket;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacketsTest
{
    @Test
    public void testPacketRoundTrip()
    {
        byte[] buffer = new byte[1024];
        setAll(buffer, i -> (byte) (i % 67));

        createPacket(buffer, 10, 10, 42);
        verifyPacket(buffer, 10, 10);

        buffer[9] = 42;
        buffer[20] = 42;
        verifyPacket(buffer, 10, 10);
    }

    @Test
    public void testVerifyPacket()
    {
        byte[] buffer = new byte[1024];
        setAll(buffer, i -> (byte) (i % 67));

        createPacket(buffer, 10, 10, 42);
        buffer[14] = 42;
        assertThatThrownBy(() -> verifyPacket(buffer, 10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Checksum failed: readHash: -697301198364534761, computedHash: -697301196770699241, full packet: F652B0412A0718173839");
    }

    private static void setAll(byte[] bytes, IntFunction<Byte> function)
    {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = function.apply(i);
        }
    }
}

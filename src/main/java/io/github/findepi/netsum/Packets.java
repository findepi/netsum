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

import io.airlift.slice.XxHash64;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.BaseEncoding.base16;
import static io.airlift.slice.Slices.wrappedBuffer;
import static io.github.findepi.netsum.Io.decodeLong;
import static io.github.findepi.netsum.Io.encodeLong;
import static java.lang.String.format;

final class Packets
{
    private Packets() {}

    private static final int HASH_LENGTH = 8;

    public static void createPacket(byte[] buffer, int offset, int length, long seed)
    {
        checkArgument(length >= HASH_LENGTH);

        for (int i = offset + HASH_LENGTH; i < offset + length; i++) {
            buffer[i] = (byte) (i ^ seed);
        }

        long hash = XxHash64.hash(wrappedBuffer(buffer, offset + HASH_LENGTH, length - HASH_LENGTH));
        encodeLong(hash, buffer, offset);
    }

    public static void verifyPacket(byte[] packet, int offset, int length)
    {
        long readHash = decodeLong(packet, offset);
        long computedHash = XxHash64.hash(wrappedBuffer(packet, offset + HASH_LENGTH, length - HASH_LENGTH));
        if (readHash != computedHash) {
            throw new IllegalArgumentException(format(
                    "Checksum failed: readHash: %s, computedHash: %s, full packet: %s",
                    readHash,
                    computedHash,
                    base16().encode(packet, offset, length)));
        }
    }
}

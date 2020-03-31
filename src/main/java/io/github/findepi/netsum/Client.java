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

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static com.google.common.io.BaseEncoding.base16;
import static com.google.common.io.ByteStreams.readFully;
import static io.airlift.concurrent.Threads.daemonThreadsNamed;
import static io.github.findepi.netsum.Io.writePacket;
import static io.github.findepi.netsum.Packets.createPacket;
import static java.lang.String.format;

@Command(name = "client")
public class Client
        implements Runnable
{
    private static final Logger log = Logger.get(Client.class);

    @Option(name = {"-h", "--host"}, required = true)
    public String host;

    @Option(name = {"-p", "--port"}, required = true)
    public int port;

    @Option(name = {"-t", "--threads"})
    public int threads = 1;

    @Option(name = {"-s", "--size"}, description = "packet size")
    private int packetLength = 100_000;

    private Throughput throughout;

    @Override
    public void run()
    {
        verify(threads > 0, "No threads: %s", threads);
        verify(packetLength > 0, "Rogue packetLength: %s", packetLength);

        ExecutorService executorService = Executors.newCachedThreadPool(daemonThreadsNamed("client-%s"));
        throughout = new Throughput(executorService);
        CompletionService<?> executor = new ExecutorCompletionService<>(executorService);

        for (int i = 0; i < threads; i++) {
            executor.submit(this::work, null);
        }

        try {
            executor.take().get();
        }
        catch (InterruptedException e) {
            log.info("Interrupted");
            System.exit(1);
        }
        catch (ExecutionException e) {
            throw new RuntimeException("Execution failed", e);
        }
    }

    private void work()
    {
        byte[] workBuffer = new byte[42];
        byte[] packetBuffer = new byte[packetLength];
        byte[] responseBuffer = new byte[packetBuffer.length];

        try (Socket socket = new Socket(host, port)) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            int round = 0;
            while (true) {
                createPacket(packetBuffer, 0, packetLength, round);
                writePacket(outputStream, workBuffer, packetBuffer, 0, packetLength);
                readFully(inputStream, responseBuffer, 0, packetLength);
                verifyResponse(responseBuffer, 0, packetLength, packetBuffer, 0, packetLength);

                throughout.add(packetLength);
                round++;
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyResponse(byte[] responseBuffer, int responseOffset, int responseLength, byte[] packetBuffer, int packetOffset, int packetLength)
    {
        checkArgument(responseLength == packetLength, "Lengths off");

        for (int i = 0; i < responseLength; i++) {
            if (responseBuffer[responseOffset + i] != packetBuffer[packetOffset + packetLength - 1 - i]) {
                RuntimeException exception = new RuntimeException(format("Malformed response at position %s", i));
                exception.addSuppressed(new Exception(format("Sent: %s", base16().encode(packetBuffer, packetOffset, packetLength))));
                exception.addSuppressed(new Exception(format("Recv: %s", base16().encode(responseBuffer, responseOffset, responseLength))));
                throw exception;
            }
        }
    }
}

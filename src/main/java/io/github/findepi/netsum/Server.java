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
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static io.airlift.concurrent.Threads.daemonThreadsNamed;
import static io.github.findepi.netsum.Io.readPacket;
import static io.github.findepi.netsum.Io.writeFully;
import static io.github.findepi.netsum.Packets.verifyPacket;

@Command(name = "server")
public class Server
        implements Runnable
{
    private static final Logger log = Logger.get(Server.class);

    @Option(name = {"-p", "--port"}, required = true)
    public int port;

    private Throughput throughout;
    private volatile boolean stopped;

    @Override
    public void run()
    {
        ExecutorService executor = Executors.newCachedThreadPool(daemonThreadsNamed("server-%s"));
        throughout = new Throughput(executor);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!stopped) {
                Socket socket = serverSocket.accept();
                try {
                    executor.submit(() -> handleClient(socket));
                }
                catch (RejectedExecutionException e) {
                    socket.close();
                    throw e;
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            stopped = true;
            executor.shutdownNow();
        }
    }

    private void handleClient(Socket socket)
    {
        try {
            log.info("Client connected: %s", socket.getRemoteSocketAddress());
            byte[] buffer = new byte[16 * 1024 * 1024];

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            while (socket.isConnected() && !stopped) {
                int length = readPacket(inputStream, buffer);
                verifyPacket(buffer, 0, length);
                reverse(buffer, 0, length);
                writeFully(outputStream, buffer, 0, length);

                throughout.add(length);
            }
        }
        catch (Throwable e) {
            log.error(e, "Client handing");
        }
    }

    private static void reverse(byte[] bytes, int offset, int length)
    {
        for (int left = offset, right = offset + length - 1; left < right; left++, right--) {
            byte b = bytes[left];
            bytes[left] = bytes[right];
            bytes[right] = b;
        }
    }
}

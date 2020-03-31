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

import io.airlift.log.Logger;
import io.airlift.stats.DecayCounter;
import io.airlift.stats.ExponentialDecay;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Throughput
{
    private static final Logger log = Logger.get(Throughput.class);

    // TODO is this the way?
    private DecayCounter counter = new DecayCounter(ExponentialDecay.oneMinute());

    public Throughput(Executor executor)
    {
        requireNonNull(executor, "executor is null").execute(this::report);
    }

    private void report()
    {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        try {
            while (true) {
                SECONDS.sleep(5);
                log.info("Rate: %s", numberFormat.format((long) counter.getRate()));
            }
        }
        catch (Throwable e) {
            log.error(e, "Reporting failed");
        }
    }

    public void add(long bytes)
    {
        counter.add(bytes);
    }
}

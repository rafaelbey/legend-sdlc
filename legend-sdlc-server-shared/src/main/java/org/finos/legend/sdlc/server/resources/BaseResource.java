// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.sdlc.server.resources;

import org.finos.legend.sdlc.server.error.MetadataException;
import org.finos.legend.sdlc.server.tools.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

public abstract class BaseResource
{
    protected <T> T executeWithLogging(String description, Supplier<T> supplier)
    {
        Logger logger = getLogger();
        boolean isInfoLogging = logger.isInfoEnabled();
        long start = System.nanoTime();
        if (isInfoLogging)
        {
            logger.info("Starting {}", description);
        }
        try
        {
            T result = supplier.get();
            if (isInfoLogging)
            {
                long duration = System.nanoTime() - start;
                StringBuilder builder = new StringBuilder(description.length() + 32).append("Finished ").append(description).append(" (");
                StringTools.formatDurationInNanos(builder, duration);
                builder.append("s)");
                logger.info(builder.toString());
            }
            return result;
        }
        catch (MetadataException e)
        {
            Status status = e.getStatus();
            if ((status != null) && (status.getFamily() == Family.REDIRECTION))
            {
                if (isInfoLogging)
                {
                    long duration = System.nanoTime() - start;
                    String redirectLocation = String.valueOf(e.getMessage());
                    StringBuilder builder = new StringBuilder(description.length() + redirectLocation.length() + 39).append("Redirected ").append(description).append(" to: ").append(redirectLocation).append(" (");
                    StringTools.formatDurationInNanos(builder, duration);
                    builder.append("s)");
                    logger.info(builder.toString());
                }
            }
            else
            {
                logError(logger, e, description, start);
            }
            throw e;
        }
        catch (Throwable t)
        {
            logError(logger, t, description, start);
            throw t;
        }
    }

    protected <T, R> R executeWithLogging(String description, Function<? super T, R> function, T arg)
    {
        return executeWithLogging(description, () -> function.apply(arg));
    }

    protected <T, U, R> R executeWithLogging(String description, BiFunction<? super T, ? super U, R> function, T arg1, U arg2)
    {
        return executeWithLogging(description, () -> function.apply(arg1, arg2));
    }

    protected void executeWithLogging(String description, Runnable runnable)
    {
        executeWithLogging(description, () ->
        {
            runnable.run();
            return null;
        });
    }

    protected <T> void executeWithLogging(String description, Consumer<? super T> consumer, T arg)
    {
        executeWithLogging(description, () ->
        {
            consumer.accept(arg);
            return null;
        });
    }

    protected <T, U> void executeWithLogging(String description, BiConsumer<? super T, ? super U> consumer, T arg1, U arg2)
    {
        executeWithLogging(description, () ->
        {
            consumer.accept(arg1, arg2);
            return null;
        });
    }

    protected Logger getLogger()
    {
        return LoggerFactory.getLogger(getClass());
    }

    private void logError(Logger logger, Throwable t, String description, long start)
    {
        if (logger.isErrorEnabled())
        {
            long duration = System.nanoTime() - start;
            StringBuilder builder = new StringBuilder(description.length() + 29).append("Error ").append(description).append(" (");
            StringTools.formatDurationInNanos(builder, duration);
            builder.append("s)");
            logger.error(builder.toString(), t);
        }
    }
}

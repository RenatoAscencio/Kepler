package org.alexdev.kepler.util.config;

import java.io.FileNotFoundException;

/**
 * Historical entry-point used to materialise a hard-coded log4j.properties on
 * the working directory and reconfigure the root logger from it. The bundled
 * src/main/resources/log4j.properties is the source of truth now and is read
 * by reload4j's default classpath discovery, so this routine is a no-op.
 *
 * Kept around to preserve the public method shape used by Kepler.main while
 * the call site is being removed in a follow-up; deleting it here would
 * break callers that compile against this class on older branches.
 */
public class LoggingConfiguration {
    public static void checkLoggingConfig() throws FileNotFoundException {
        // Intentionally empty. See the class-level Javadoc.
    }
}

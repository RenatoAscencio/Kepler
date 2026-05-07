package org.alexdev.kepler.util.observability;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialises the Sentry SDK at process start.
 *
 * Sentry is opt-in via the SENTRY_DSN environment variable; when it's unset
 * (local development, CI) the bootstrap is a no-op and the rest of the server
 * runs unchanged. When it is set, every uncaught exception on the main thread
 * and on Netty/scheduler worker threads gets captured with the breadcrumbs
 * the SDK has accumulated since boot, plus the static tags (release SHA,
 * environment, server version) wired below.
 *
 * The bootstrap is intentionally synchronous and idempotent so the rest of
 * Kepler.main can call Sentry.captureException at any catch site without
 * having to coordinate with a separate init phase.
 */
public final class SentryBootstrap {
    private static final Logger log = LoggerFactory.getLogger(SentryBootstrap.class);

    private SentryBootstrap() {
    }

    /**
     * Initialise Sentry from environment variables.
     *
     * <ul>
     *   <li>{@code SENTRY_DSN} — required to enable Sentry. Format:
     *       {@code http://<key>@<host>:<port>/<project_id>}.</li>
     *   <li>{@code SENTRY_RELEASE} — recommended. Git SHA of the build.</li>
     *   <li>{@code SENTRY_ENVIRONMENT} — defaults to {@code production}.</li>
     *   <li>{@code SENTRY_SAMPLE_RATE} — fraction of error events sent
     *       upstream. Defaults to 1.0 (everything). Range [0.0, 1.0].</li>
     * </ul>
     */
    public static void init(String serverVersion) {
        String dsn = systemPropertyOrEnv("sentry.dsn", "SENTRY_DSN");

        if (dsn == null || dsn.isBlank()) {
            log.info("Sentry not configured (SENTRY_DSN unset); error tracking disabled");
            return;
        }

        String release = systemPropertyOrEnv("sentry.release", "SENTRY_RELEASE");
        String environment = systemPropertyOrEnv("sentry.environment", "SENTRY_ENVIRONMENT");
        if (environment == null || environment.isBlank()) {
            environment = "production";
        }

        Double sampleRate = parseDouble(
                systemPropertyOrEnv("sentry.sample.rate", "SENTRY_SAMPLE_RATE"));

        SentryOptions options = new SentryOptions();
        options.setDsn(dsn);
        options.setEnvironment(environment);
        if (release != null && !release.isBlank()) {
            options.setRelease(release);
        }
        if (sampleRate != null) {
            options.setSampleRate(sampleRate);
        }
        options.setAttachStacktrace(true);
        options.setAttachServerName(true);
        options.setEnableExternalConfiguration(true);
        options.setMaxBreadcrumbs(200);
        options.setDebug(false);
        options.setSendDefaultPii(false);

        Sentry.init(options);
        Sentry.configureScope(scope -> {
            scope.setTag("component", "kepler-server");
            if (serverVersion != null) {
                scope.setTag("server_version", serverVersion);
            }
        });

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Tag the event with the offending thread name so we can group
            // worker-thread crashes separately from main-thread ones.
            Sentry.withScope(scope -> {
                scope.setTag("thread", thread.getName());
                Sentry.captureException(throwable);
            });
            // Preserve the legacy behaviour of also surfacing the trace to the
            // server logs — Sentry capture is additive, not a replacement.
            log.error("Uncaught exception on thread {}", thread.getName(), throwable);
        });

        Sentry.captureMessage("Kepler started", SentryLevel.INFO);
        log.info("Sentry initialised (environment={}, release={}, sampleRate={})",
                environment,
                release == null ? "none" : release,
                sampleRate == null ? "1.0" : sampleRate.toString());
    }

    private static String systemPropertyOrEnv(String propertyKey, String envKey) {
        String value = System.getProperty(propertyKey);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = System.getenv(envKey);
        return value == null ? null : value.trim();
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(raw);
            if (parsed < 0.0) {
                return 0.0;
            }
            if (parsed > 1.0) {
                return 1.0;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

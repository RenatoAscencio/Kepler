#!/bin/sh
# kepler.log.dir is consumed by log4j.properties to anchor the rolling file
# appender. The Dockerfile creates and chowns /var/log/kepler; bind-mount
# /opt/kepler-logs:/var/log/kepler in production so log files survive
# container replacement and can be tailed/shipped from the host.
LOG_DIR="${KEPLER_LOG_DIR:-/var/log/kepler}"
mkdir -p "$LOG_DIR" 2>/dev/null || true

exec java \
    --enable-native-access=ALL-UNNAMED \
    -Dkepler.log.dir="$LOG_DIR" \
    -classpath "kepler.jar:lib/*" \
    org.alexdev.kepler.Kepler

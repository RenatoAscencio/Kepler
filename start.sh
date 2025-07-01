#!/bin/sh
# Startup script for EasyPanel and Docker

# Print URL for connecting to the Kepler server
HOST=${SERVER_HOST:-localhost}
PORT=${SERVER_PORT:-12321}

cat <<MSG
Kepler se está iniciando...
Podrás conectarte a tu hotel en: http://${HOST}:${PORT}
MSG

# Ejecutar script existente de arranque
exec sh run.sh


##############
# BASE STAGE #
##############

FROM alpine:3.20 AS base

# Add OpenJDK17
RUN apk add --no-cache openjdk17

# Uses /kepler directory
WORKDIR /kepler

###############
# BUILD STAGE #
###############

FROM gradle:7.6-jdk17-alpine AS build

# Working directory for gradle image is /home/gradle by default
WORKDIR /kepler

# Install unzip utility
RUN apk add --no-cache unzip

# Copy every files/folders that are not in .dockerignore
COPY . .

# Convert CRLF to LF executable files (failing build for Windows without this)
RUN sed -i 's/\r$//' tools/scripts/run.sh

# Make run.sh executable
RUN chmod +x tools/scripts/run.sh

# Run gradle build
RUN gradle distZip --no-daemon

# Unzip builded Kepler server
RUN unzip -qq ./Kepler-Server/build/distributions/Kepler-Server.zip -d ./release

# Prepare build directory
RUN rm -rf ./release/Kepler-Server/bin && \
    mkdir -p ./build/lib && \
    mv ./release/Kepler-Server/lib/Kepler-Server.jar ./build/kepler.jar && \
    mv ./release/Kepler-Server/lib/* ./build/lib && \
    cp tools/scripts/run.sh ./build/

####################
# PRODUCTION STAGE #
####################

FROM base AS production

# Copy builded Kepler server
COPY --from=build /kepler/build ./

# Default ports (can be overridden by environment variables)
ENV SERVER_PORT=12321 \
    RCON_PORT=12309 \
    MUS_PORT=12322

# Expose ports for the emulator, RCON and MUS servers
EXPOSE ${SERVER_PORT} ${RCON_PORT} ${MUS_PORT}

COPY start.sh ./
ENTRYPOINT ["sh", "start.sh"]


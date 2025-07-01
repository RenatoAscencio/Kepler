#!/bin/sh
# Simple helper to build the Kepler Docker image and run it with MariaDB without docker-compose

set -e

IMAGE_NAME=kepler
DB_CONTAINER=kepler-db
APP_CONTAINER=kepler

# Build Kepler image

docker build -t $IMAGE_NAME .

# Start MariaDB container if not already running
if [ ! "$(docker ps -q -f name=$DB_CONTAINER)" ]; then
  if [ "$(docker ps -aq -f status=exited -f name=$DB_CONTAINER)" ]; then
    docker rm $DB_CONTAINER
  fi
  docker run -d --name $DB_CONTAINER \
    -e MARIADB_ROOT_PASSWORD=veryverysecret \
    -e MYSQL_DATABASE=kepler \
    -e MYSQL_USER=kepler \
    -e MYSQL_PASSWORD=verysecret \
    -v "$(pwd)/tools/kepler.sql:/docker-entrypoint-initdb.d/kepler.sql" \
    mariadb:11.4
fi

# Start Kepler container, removing any old one
if [ "$(docker ps -aq -f name=$APP_CONTAINER)" ]; then
  docker rm -f $APP_CONTAINER
fi

docker run -d --name $APP_CONTAINER --link $DB_CONTAINER:mariadb \
  -p 12321:12321 -p 12309:12309 -p 12322:12322 \
  -e MYSQL_HOSTNAME=mariadb \
  $IMAGE_NAME


#!/usr/bin/env bash
# removes unused containers, images, networks and volumes
set -ex

docker container prune -f
docker image prune -f
docker network prune -f
docker volume prune -f
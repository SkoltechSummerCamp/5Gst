#!/bin/bash
set -e
# INSERT YOUR PUBLIC IP HERE
export YOUR_IP=${}

echo "## Load Docker images from *.tar"
docker load -i service.tar.gz
docker load -i balancer.tar.gz
docker load -i postgres.tar.gz

echo "## Allowed IP: " $YOUR_IP

echo "## Start Docker Compose"
docker compose up -d

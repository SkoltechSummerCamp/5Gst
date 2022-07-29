#!/bin/bash

mkdir 5GST
cd 5GST

echo "pulling docker images"
docker pull skoltech/speedtest-service:${SERVICE_VERSION:-0.3.0.80}
docker pull skoltech/speedtest-balancer:${BALANCER_VERSION:-0.3.0.80}
docker pull postgres:${POSTGRES_VERSION:-14.4}
echo "start making .tar files"
docker save -o service.tar skoltech/speedtest-service:${SERVICE_VERSION:-0.3.0.80}
docker save -o balancer.tar skoltech/speedtest-balancer:${BALANCER_VERSION:-0.3.0.80}
docker save -o postgres.tar postgres:${POSTGRES_VERSION:-14.4}

echo "install script to convert .tar files to images"

wget --no-check-certificate --content-disposition https://raw.githubusercontent.com/SkoltechSummerCamp/5Gst/develop/docker_unpack.sh

echo "install docker-compose"

wget --no-check-certificate --content-disposition https://raw.githubusercontent.com/SkoltechSummerCamp/5Gst/develop/docker-compose.yml

echo "all images downladed"

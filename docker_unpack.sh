#!/bin/bash

echo "load images from .tar"
docker load -i service.tar.gz
docker load -i balancer.tar.gz 
docker load -i postgres.tar.gz 

echo "start docker compose"
docker compose up -d

echo "all images downladed to your usb drive"

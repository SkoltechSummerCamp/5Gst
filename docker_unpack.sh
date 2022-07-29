#!/bin/bash

echo "load images from .tar"
docker load -i service.tar 
docker load -i balancer.tar 
docker load -i postgres.tar 

echo "start docker compose"
docker compose up -d

echo "all images downladed to your usb drive"

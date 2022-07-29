#!/bin/bash

echo "load images from .tar"
docker load -i service.tar 
docker load -i balancer.tar 
docker load -i postgres.tar 

echo "all images downladed to your usb drive"

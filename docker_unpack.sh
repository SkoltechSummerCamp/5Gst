#!/bin/bash

mkdir 5GST
cd 5GST

echo "load images from .tar"
docker load -i service.tar 
docker load -i balancer.tar 
docker load -i postgres.tar 

cd $OLDPWD
rm -r 5GST
echo "all images downladed to your usb drive"

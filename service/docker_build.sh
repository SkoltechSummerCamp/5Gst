#!/bin/sh

cp -r ../iPerf .
docker build -t docker_service .
rm -rdf ./iPerf

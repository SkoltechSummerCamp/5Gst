#!/bin/sh

cp -r ../iPerf .
docker build -t kek .
rm -rdf ./iPerf
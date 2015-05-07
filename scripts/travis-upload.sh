#!/usr/bin/env bash

# Upload a build to files.smssecure.org
# usage: ./travis-upload <key>

set -eo pipefail

if [ "$#" -lt 1 ]; then
    echo "usage: ./travis-upload <key>"
    exit 1
fi

if [ `basename $(pwd)` = "scripts" ]; then
    cd ..
fi

if [ ! -f "./build/outputs/apk/SMSSecure-debug.apk" ]; then
    echo "APK not found. Did you run `./gradlew build`?"
    exit 1
fi

COMMIT=$(git rev-parse --short HEAD)
curl --form "fileupload=@./build/outputs/apk/SMSSecure-debug.apk;filename=SMSSecure-debug-$COMMIT.apk" -H "Authorization: KEY $1" https://files.smssecure.org/

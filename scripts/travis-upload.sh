#!/usr/bin/env bash

# Upload a build to files.silence.im
# usage: ./scripts/travis-upload.sh <key>

set -eo pipefail

if [ "$#" -lt 1 ]; then
    echo "usage: ./scripts/travis-upload <key>"
    exit 1
fi

if [ `basename $(pwd)` = "scripts" ]; then
    cd ..
fi

if [ ! -f "./build/outputs/apk/Silence-debug.apk" ]; then
    echo "APK not found. Did you run `./gradlew build`?"
    exit 1
fi

COMMIT=$(git rev-parse --short HEAD)
BRANCH=$(git rev-parse --abbrev-ref HEAD)
curl --form "fileupload=@./build/outputs/apk/Silence-debug.apk;filename=Silence-debug-$BRANCH-$COMMIT.apk" -H "Authorization: KEY $1" https://files.silence.im/

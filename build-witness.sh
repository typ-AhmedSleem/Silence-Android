#!/usr/bin/env bash

rm ./libs/gradle-witness.jar

set -eo pipefail

pushd ./libs/gradle-witness
gradle jar
popd
cp ./libs/gradle-witness/build/libs/gradle-witness.jar ./libs/gradle-witness.jar

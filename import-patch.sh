#!/usr/bin/env bash

# Import a patch from TextSecure and adapt it for SMSSecure
# ../TextSecure must be an cloned git tree
# usage: ./import-patch.sh ID

set -eo pipefail

if [[ $1 == "" ]]; then
    echo "usage: ./import-patch.sh ID"
    exit 1
fi

pushd ../TextSecure > /dev/null
git pull origin master > /dev/null
git checkout $1 2> /dev/null
git format-patch -1 --minimal 2> /dev/null
git checkout master > /dev/null 2>&1
popd > /dev/null
mv ../TextSecure/*.patch .

./fix-patch.sh

#!/usr/bin/env bash

# Import a patch from TextSecure and adapt it for Silence
# ../Signal-Android must be an cloned git tree
# usage: ./scripts/import-patch.sh <commit SHAs>

set -eo pipefail

if [ "$#" -lt 1 ]; then
    echo "usage: ./scripts/import-patch.sh <commit SHAs>"
    exit 1
fi

if [ `basename $(pwd)` = "scripts" ]; then
    cd ..
fi

cwd=`pwd`

if [ ! -d "../Signal-Android" ]; then
    echo "Signal-Android repo not found (clone it to $cwd/../Signal-Android)"
    exit 1
fi

cd ../Signal-Android
git pull origin master > /dev/null 2>&1

for sha in "$@"; do
    git checkout "$sha" 2> /dev/null
    git format-patch -1 --minimal --stdout > "$cwd/$sha.patch" 2> /dev/null
    $cwd/scripts/fix-patch.sh "$cwd/$sha.patch"
done

git checkout master > /dev/null 2>&1

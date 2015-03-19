#!/usr/bin/env bash

find . -name "*.patch" -print | xargs sed -i 's/thoughtcrime\/securesms/SecuredText\/SecuredText/g'
find . -name "*.patch" -print | xargs sed -i 's/org.thoughtcrime.securesms/org.SecuredText.SecuredText/g'
find . -name "*.patch" -print | xargs sed -i 's/org.SecuredText.SecuredText.util.TextSecure/org.SecuredText.SecuredText.util.SecuredText/g'

#!/usr/bin/env bash

find . -name "*.patch" -print | xargs sed -i 's/thoughtcrime\/securesms/smssecure\/smssecure/g'
find . -name "*.patch" -print | xargs sed -i 's/org.thoughtcrime.securesms/org.smssecure.smssecure/g'
find . -name "*.patch" -print | xargs sed -i 's/org.smssecure.smssecure.util.TextSecure/org.smssecure.smssecure.util.SMSSecure/g'
find . -name "*.patch" -print | xargs sed -i 's/TextSecurePreferences/SMSSecurePreferences/g'
find . -name "*.patch" -print | xargs sed -i 's/^[Ff]ixes #/Fixes https:\/\/github.com\/WhisperSystems\/TextSecure\/issues\//g'
find . -name "*.patch" -print | xargs sed -i 's/^[Cc]loses #/Closes https:\/\/github.com\/WhisperSystems\/TextSecure\/pull\//g'

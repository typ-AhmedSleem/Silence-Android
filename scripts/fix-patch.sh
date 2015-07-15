#!/usr/bin/env bash

if [ "$#" -lt 1 ]; then
    echo "usage: ./fix-patch.sh <files>"
    exit 1
fi

for file in "$@"; do
    sed -i -e 's/thoughtcrime\/securesms/smssecure\/smssecure/g' \
           -e 's/org.thoughtcrime.securesms/org.smssecure.smssecure/g' \
           -e 's/org.thoughtcrime.provider.securesms/org.smssecure.provider.smssecure/g' \
           -e 's/org.smssecure.smssecure.util.TextSecure/org.smssecure.smssecure.util.SMSSecure/g' \
           -e 's/TextSecurePreferences/SMSSecurePreferences/g' \
           -e 's/TextSecureTestCase/SMSSecureTestCase/g' \
           -e 's/TextSecure.LightNoActionBar/SMSSecure.LightNoActionBar/g' \
           -e 's/TextSecure.LightActionBar/SMSSecure.LightActionBar/g' \
           -e 's/TextSecure.DarkNoActionBar/SMSSecure.DarkNoActionBar/g' \
           -e 's/TextSecure.DarkActionBar/SMSSecure.DarkActionBar/g' \
           -e 's/TextSecure.LightTheme/SMSSecure.LightTheme/g' \
           -e 's/TextSecure.DarkTheme/SMSSecure.DarkTheme/g' \
           -e 's/TextSecure.TitleTextStyle/SMSSecure.TitleTextStyle/g' \
           -e 's/^[Ff]ixes #/Fixes https:\/\/github.com\/WhisperSystems\/TextSecure\/issues\//g' \
           -e 's/^[Cc]loses #/Closes https:\/\/github.com\/WhisperSystems\/TextSecure\/pull\//g' \
           "$file"
done

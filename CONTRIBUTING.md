## Translations

Please do not submit issues or pull requests for translation fixes. Anyone can update the translations in [Transifex](https://www.transifex.com/projects/p/smssecure/).
Please submit your corrections there.


## Submitting bug reports

1. Search our issues first to make sure this is not a duplicate.
2. (Optional) Search [Signal's issues](https://github.com/WhisperSystems/Signal-Android/issues).
3. Open an issue with:
  * Device and app information
    * What's your device?
    * What Android version is it running?
    * What version and build ID of Silence do you have?
  * Upstream bug (if any)
  * App state
    * What are the relevant preferences that you have set that may be related to the issue?
  * Raw debug info
    * A debug log (Settings → Advanced → Submit debug log, or `adb logcat | grep $(adb shell ps | grep org.smssecure.smssecure | cut -c10-15)`). If your debug log contains sensitive data, you can send it at support@smssecure.org.
    * (Optional) Screenshots (`adb shell /system/bin/screencap -p /sdcard/screencap.png && adb pull /sdcard/screencap.png`) or video.

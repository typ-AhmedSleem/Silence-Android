# SecuredText

SecuredText is a messaging app for simple private communication with friends to communicate securely using SMS. This is a fork of [TextSecure](https://github.com/WhisperSystems/TextSecure), in which SMS encryption is not available anymore.

## Goals of this fork

SecuredText focuses on SMS. This fork aims to:

* Keep SMS encryption
* Drop Google services dependencies: push messages are not available in SecuredText; if you want to keep them, SecuredText is not for you.

## Migrating from TextSecure to SecuredText

* In TextSecure, export plaintext backup. Warning: the backup will **not** be encrypted.
* Rename `TextSecurePlaintextBackup.xml` to `SecuredTextPlaintextBackup.xml`.
* Remove TextSecure.
* Install SecuredText.
* In SecuredText, import plaintext backup.
* Enjoy SecuredText!

Note: You will have to start new secured sessions.

## Contributing Bug reports
We use GitHub for bug tracking. Please search the existing issues for your bug and create a new one if the issue is not yet tracked!

https://github.com/SecuredText/SecuredText/issues

## Contributing Code
Instructions on how to setup your development environment and build SecuredText can be found in  [BUILDING.md](https://github.com/SecuredText/SecuredText/blob/master/BUILDING.md).

If you're new to the SecuredText codebase, we recommend going through our issues and picking out a simple bug to fix (check the "easy" label in our issues) in order to get yourself familiar.

Help
====
## Documentation
Looking for documentation? Check out the wiki of the original project:

https://github.com/WhisperSystems/TextSecure/wiki

# Legal things
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.
The form and manner of this distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

## License

Copyright 2011 Whisper Systems

Copyright 2013-2014 Open Whisper Systems

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

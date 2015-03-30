# SMSSecure [![Build Status](https://travis-ci.org/SMSSecure/SMSSecure.svg?branch=master)](https://travis-ci.org/SMSSecure/SMSSecure)

SMSSecure is a messaging app for simple private communication with friends to communicate securely using SMS. This is a fork of [TextSecure](https://github.com/WhisperSystems/TextSecure), in which SMS encryption is not available anymore.

## Goals of this fork

SMSSecure focuses on SMS. This fork aims to:

* Keep SMS encryption
* Drop Google services dependencies: push messages are not available in SMSSecure; if you want to keep them, SMSSecure is not for you.

## Migrating from TextSecure to SMSSecure

* In TextSecure, export plaintext backup. Warning: the backup will **not** be encrypted.
* Rename `TextSecurePlaintextBackup.xml` to `SMSSecurePlaintextBackup.xml`.
* Install SMSSecure.
* In SMSSecure, import plaintext backup.
* Enjoy SMSSecure!

Note: You will have to start new secured sessions.

## Contributing Bug reports
We use GitHub for bug tracking. Please search the existing issues for your bug and create a new one if the issue is not yet tracked!

https://github.com/SMSSecure/SMSSecure/issues

## Contributing Translations
We use Transifex for our translations. If you'd like to contribute, the project is here:

https://www.transifex.com/projects/p/smssecure/

## Contributing Code
Instructions on how to setup your development environment and build SMSSecure can be found in  [BUILDING.md](https://github.com/SMSSecure/SMSSecure/blob/master/BUILDING.md).

If you're new to the SMSSecure codebase, we recommend going through our issues and picking out a simple bug to fix (check the "easy" label in our issues) in order to get yourself familiar.

Help
====
## Documentation
Looking for documentation? Check out the wiki of the original project:

https://github.com/WhisperSystems/TextSecure/wiki

## Chat
Have a question? Want to help out? Join our IRC channel: [#SMSSecure on Freenode](https://webchat.freenode.net/?channels=SMSSecure)

## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

## License

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

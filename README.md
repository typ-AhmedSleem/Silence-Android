# SMSSecure [![Build Status](https://travis-ci.org/SMSSecure/SMSSecure.svg?branch=master)](https://travis-ci.org/SMSSecure/SMSSecure)

[SMSSecure](https://smssecure.org) is an SMS/MMS application that allows you to protect your privacy while communicating with friends.

Using SMSSecure, you can send SMS messages and share media or attachments with complete privacy.

Features:
* Easy. SMSSecure works like any other SMS application. There's nothing to sign up for and no new service your friends need to join.
* Reliable. SMSSecure communicates using encrypted SMS messages. No servers or internet connection required.
* Private. SMSSecure uses the TextSecure encryption protocol to provide privacy for every message, every time.
* Safe. All messages are encrypted locally, so if your phone is lost or stolen, your messages are protected.
* Open Source. SMSSecure is Free and Open Source, enabling anyone to verify its security by auditing the code.


## Project goals

This is a fork of [TextSecure](https://github.com/WhisperSystems/TextSecure) that aims to keep the SMS encryption that TextSecure removed [for a variety of reasons](https://whispersystems.org/blog/goodbye-encrypted-sms/).

SMSSecure focuses on SMS and MMS. This fork aims to:

* Keep SMS/MMS encryption
* Drop Google services dependencies (push messages are not available in SMSSecure)
* Integrate upstream bugfixes and patches from TextSecure

## Migrating from TextSecure to SMSSecure

* In TextSecure, export a plaintext backup. Warning: the backup will **not** be encrypted.
* Install SMSSecure.
* In SMSSecure, import he plaintext backup (this will import the TextSecure backup if no SMSSecure backup is found).
* Enjoy SMSSecure!

Note: You will have to start new secured sessions with your contacts.

# Contributing

See [CONTRIBUTING.md](https://github.com/SMSSecure/SMSSecure/blob/master/CONTRIBUTING.md) for how to contribute code, translations, or bug reports.

Instructions on how to setup a development environment and build SMSSecure can be found in [BUILDING.md](https://github.com/SMSSecure/SMSSecure/blob/master/BUILDING.md).

# Help
## Documentation
Looking for documentation? Check out the wiki of the original project:

https://github.com/WhisperSystems/TextSecure/wiki

## Chat
Have a question? Want to help out? Join our IRC channel: [#SMSSecure on Freenode](https://webchat.freenode.net/?channels=SMSSecure)

# Legal
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

## License

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

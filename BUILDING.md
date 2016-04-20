Building Silence
================

Basics
------

Silence uses [Gradle](http://gradle.org) to build the project and to maintain
dependencies.

Building Silence
----------------

The following steps should help you (re)build Silence from the command line.

1. Checkout the source somewhere on your filesystem with

        git clone --recursive https://github.com/SilenceIM/Silence.git

2. Make sure you have the [Android SDK](https://developer.android.com/sdk/index.html) installed somewhere on your system.
3. Ensure that the following packages are installed from the Android SDK manager:
    * Android SDK Build Tools
    * SDK Platform
    * Android Support Repository
    * Google Repository
4. Create a local.properties file at the root of your source checkout and add an sdk.dir entry to it.

        sdk.dir=\<path to your sdk installation\>

5. (Optional) Build [Gradle-Witness](https://github.com/WhisperSystems/gradle-witness)

        ./scripts/build-witness.sh

6. Execute Gradle:

        ./gradlew build

If you get a `Configuration with name 'default' not found.`, please update submodules:

        git submodule init && git submodule update

Visual assets
-------------

Sample command for generating our audio placeholder image:

```bash
pngs_from_svg.py ic_audio.svg /path/to/Silence/res/ 150 --color #000 --opacity 0.54 --suffix _light
pngs_from_svg.py ic_audio.svg /path/to/Silence/res/ 150 --color #fff --opacity 1.00 --suffix _light
```


Translations
------------

Install the [Transifex Client](http://docs.transifex.com/developer/client/setup) to update project strings

Pull down all new translation updates:
 - `tx pull -af --minimum-perc=1`

Pull down specific locales:
 - `tx pull -l <locales>`

Push updated default strings:
 - `tx push -s`

Push specific locales:
 - `tx push -t -l <locales>`

Changing the source strings (this is a pain):
 1. Make the string change
 2. Pull the latest translations - `tx pull -af --minimum-perc=1`
 3. Push your latest source strings - `tx push -s` (this will delete all the translations of the source strings you changed)
 4. Push your local translations - `tx push -t` (this will restore the deleted translations)

 NOTES:
   - If anyone knows of a better way to do this, please contribute it, this way sucks.
   - This should only be done where the meaning of the source string doesn't change (ie. fixing a typo/rewording).
   - This will cause the restored translations to look like they're from you, not the original translator.


Full documentation at <http://docs.transifex.com/developer/client/>

Setting up a development environment
------------------------------------

[Android Studio](https://developer.android.com/sdk/installing/studio.html) is the recommended development environment.

1. Install Android Studio.
2. Make sure the "Android Support Repository" is installed in the Android Studio SDK.
3. Make sure the latest "Android SDK build-tools" is installed in the Android Studio SDK.
4. Create a new Android Studio project. from the Quickstart pannel (use File > Close Project to see it), choose "Checkout from Version Control" then "git".
5. Paste the URL for the Silence project when prompted (https://github.com/SilenceIM/Silence.git).
6. Android studio should detect the presence of a project file and ask you whether to open it. Click "yes".
7. Default config options should be good enough.
8. Project initialisation and build should proceed.

Contributing code
-----------------

Code contributions should be sent via github as pull requests, from feature branches [as explained here](https://help.github.com/articles/using-pull-requests).

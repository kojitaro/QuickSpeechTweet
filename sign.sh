#!/bin/bash

export LC_ALL=C
rm -f QuickSpeechTweet.apk
ant clean
ant release
jarsigner -keystore  android.keystore  -verbose  bin/QuickSpeechTweet-unsigned.apk kouji
zipalign -v 4 bin/QuickSpeechTweet-unsigned.apk QuickSpeechTweet.apk

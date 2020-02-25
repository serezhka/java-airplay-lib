# java-airplay-lib

[![Build Status](https://travis-ci.com/serezhka/java-airplay-lib.svg?branch=master)](https://travis-ci.com/serezhka/java-airplay-lib) [![Release](https://jitpack.io/v/serezhka/java-airplay-lib.svg)](https://jitpack.io/#serezhka/java-airplay-lib) [![HitCount](http://hits.dwyl.io/serezhka/java-airplay-lib.svg)](http://hits.dwyl.io/serezhka/java-airplay-lib)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://opensource.org/licenses/MIT)

This library is intended to easily create AirPlay2 servers acting like Apple TV. Tested with iPhone X (iOS 13.3)

## How to use?

* Add java-airplay-lib [dependency](https://jitpack.io/#serezhka/java-airplay-lib) to your project

* Make your server discoverable by [Bonjour](https://ru.wikipedia.org/wiki/Bonjour)

```java
  String serverName = "@srzhka";
  int airPlayPort = 5001;
  int airTunesPort = 7001;
  AirPlayBonjour airPlayBonjour = new AirPlayBonjour(serverName);
  airPlayBonjour.start(airPlayPort, airTunesPort);
  ...
  airPlayBonjour.stop();
```

<img src="https://github.com/serezhka/java-airplay-lib/blob/media/bonjour.jpg" width="256" height="256">

* Listen airTunesPort and handle RTSP requests. Pass request content bytes to the library and respond with provided content bytes.

```java

  RTSP GET | POST

  String uri = ...
  byte[] requestContent = ...
  switch (uri) {
    case "/info": {
      airPlay.info(.. byte output stream ..);
      // RTSP OK + provided bytes 
    }
    case "/pair-setup": {
      airPlay.pairSetup(.. byte output stream ..);
      // RTSP OK + provided bytes 
    }
    case "/pair-verify": {
      airPlay.pairVerify(.. requestContent input stream ..,
        .. byte output stream ..);
      // RTSP OK + provided bytes 
    }
    case "/fp-setup": {
      airPlay.fairPlaySetup(.. requestContent input stream ..,
        .. byte output stream ..);
      // RTSP OK + provided bytes
    }
    case "/feedback": {
      // RTSP OK
    }
  }
  
  RTSP SETUP
  
    airPlay.rtspSetup(.. requestContent input stream ..,
      .. byte output stream .., int videoDataPort, int videoEventPort,
      int videoTimingPort, int audioDataPort, int audioControlPort); 
    // RTSP OK + provided bytes
      
    if (airPlay.isFairPlayReady()) {
      // start listening mirror data on airPlayPort 
    }
  
  RTSP GET_PARAMETER, RECORD, SET_PARAMETER, TEARDOWN
  
  ...
  
  DECRYPT MIRROR DATA
    
    airPlay.fairPlayDecryptVideoData(byte[] videoData);
    
    airPlay.fairPlayDecryptAudioData(byte[] audioData);
```
<img src="https://github.com/serezhka/java-airplay-lib/blob/media/paired_1.jpg" width="256" height="256"><img src="https://github.com/serezhka/java-airplay-lib/blob/media/paired_2.jpg" height="256">

## Example server

[java-airplay-server](https://github.com/serezhka/java-airplay-server) with Netty

Currently supports only mirroring with no sound.

<img src="https://github.com/serezhka/java-airplay-server-examples/blob/media/gstreamer_playback.gif" width="600">

## Links

[Analysis of AirPlay2 Technology](http://www.programmersought.com/article/2084789418/)

## Info

Inspired by many other open source projects analyzing AirPlay2 protocol. Special thanks to OmgHax.c's author 🤯

It took me several months of sleepless nights with debugger and wireshark to make this work.

If you appreciate my work, consider buying me a cup of coffee to keep me recharged

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/paypalme2/srzhka) [![Donate](https://github.com/serezhka/java-airplay-lib/blob/media/yandex_money.svg)](https://money.yandex.ru/to/4100111540466689)

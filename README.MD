## Description
This project is part of the Systems Support for Continuous Media module in NUS. The requirement is to create a system for video streaming in two modes: Live Stream and Video-on-Demand (VOD). The project is composed of three components:
* Android app for video recording
* Android app for video playback
* Backend server for uploading, transcoding, and serving videos

The full requirement can be found on the [module's website](http://www.comp.nus.edu.sg/~cs5248/proj.html).

The Android app is hosted [here](https://github.com/tonylpt/NUS-VideoStreaming-Server-Flask).

## Implementation

![Screenshot](/screenshot-android.jpg?raw=true "Screenshot")

### Streaming

During a streaming session, each video segment (streamlet) is downloaded in timely order. Depending on the downloading speed of the previous segments, a suitable quality level will be selected for the next segment to download (adaptive bitrate).

The streamlets are played in sequence by alternating between SurfaceViews. In the[original implementation](/app/src/main/java/com/cs5248/android/ui/StreamingActivity.java)of the streamer, the surface views were created for each streamlet and destroyed after the streamlet ends. This created a gap between the playback of the streamlets. After the submission, I created an[improved version](/app/src/main/java/com/cs5248/android/ui/StreamingActivity2.java)of the streamer, which enhanced this behavior by reusing a set of created surface views. There is still a small gap when switching between the surface views. It still can be improved further.


### Job Queue

During recording, all video segments must be uploaded to the server as the recording is in progress (for Live Streaming purpose). As per requirements, the app must make sure that all segments are uploaded successfully.

In our implementation, we realized that during Live Streaming, any segment that fails to upload should not be re-uploaded immediately, in order to save the bandwidth for the latest current segment with a higher priority. We used two job queues for this purpose: one for urgent tasks (including the latest segments, which have a high priority), and one for less urgent ones (re-uploads of failed segments, and other update tasks). During a Live Streaming session, the less urgent job queue is paused temporarily and will be resumed after the recording session ends. A job published into these queues will be persisted into disk so it can be processed if the app crashes.


### Dependencies

This project was built on these awesome libraries:
* [Dagger 2](http://google.github.io/dagger/)for dependency injection
* [RxJava](https://github.com/ReactiveX/RxJava)and[RxAndroid](https://github.com/ReactiveX/RxAndroid)for threading and scheduling
* [Retrofit](http://square.github.io/retrofit/)to interface with our REST API
* [Path's Priority Job Queue](https://github.com/yigit/android-priority-jobqueue)for the job queues
* [Parceler](https://github.com/johncarl81/parceler)for object serialization in Android
* [Lombok](https://projectlombok.org/)to keep the code clean of Getters and Setters
* [JakeWharton's Butterknife](https://github.com/JakeWharton/butterknife)for view injection
* And[many others...](/app/build.gradle)

## Build

1. Install the latest Android Studio
2. Install[Lombok Plugin](https://github.com/mplushnikov/lombok-intellij-plugin)in Android Studio
3. Open the project in Android Studio by importing [build.gradle](/app/build.gradle).
3. Update the URL of the server in[Config.java](/app/src/main/java/com/cs5248/android/Config.java)


## Run

1. Make sure the server is running.
2. Deploy and start the app.

## Team Members
* Tony Luong (lpthanh{at}gmail.com)
* Randy Tandriansyah (randytandriansyah{at}gmail.com)

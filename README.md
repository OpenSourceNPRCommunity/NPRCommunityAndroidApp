# THIS IS NOT AFFILIATED TO NPR IN ANY CAPACITY. This is a community project and an avid fan that wants a better android app

# Open Source NPR Community Android App

The point of this app is to make the Android experience for NPR cleaner and more user friendly
adding in queues as well as caching for images and files locally to remove the terrible buffering
that the NPR One app has. This is just some of problems with a few enhancements like adding in a
queue.

This repo needs a lot more love so please feel free to contribute. Below is a list of features
I have been working on and plan on working on. This app in its current form is not read for 
production.

## Using this app

You need to setup your own Auth2.0 Server. Lucky for you I already have done that if you go to
my other repo [here](https://github.com/OpenSourceNPRCommunity/oauth2_proxy) and set this up you should be good to go!

After that simply clone this repo and start messing around.

I use a Samsung S8, so that is my targeted platform.

## In the works: Some thins I would like to do and are planning on doing
__
Overall Goals:
- Refactoring a lot of the Code
- Testing - I need to add it 

Immediate Goals:
- [X] Auto loading of next items in queue (will load next 4 from recommendations)
- [X] Set up sending updates to npr for progress of audio
- [X] Set auto channel items
- [X] Select Station
- [X] Sponsorship disable skipping
- [X] Logout
- [X] Images delete old version with job scheduler
    - [X] On Logout delete local content
- [X] Debug Version Check option
- Add time and date of podcast to queue and fragment info
- Add size of file to queue
- Check for internet and timeout, display errors for HTTP Calls
- Disable sound if not already playing even though is open
- Add in activity thing in dropdown
- Add in activity thing on off screen
- Add waiting to audio files that are in the queue

### Long Term not as important goals, but have been on my mind...

- [X] Some type of Icon
- Pause Button Bluetooth

- Testing
    - Any sort of testing... got to get on that for some stability. I have been more interested 
    in developing features for now... I know that is not the best form of coding, but I will 
    eventually get onto testing. 

- Login Activity
    - [ ] Clean up the forwarding for on success instead of just accepting 404 error

- Navigate Activity (Main Activity)
    - [X] Surround queue and shows Pager in Fragment to allow for screen change
    - [X] Implement nice screen back and forth for min/max of the listening page
    - [ ] Update Notify item changed instead of notify data set
    
- Queue Fragment
    - [X] Update Tiles to show a tile fragment when loading
    - [X] Implement progress visual when download and when downloaded completely

- Shows Fragment
    - [ ] Implement loading when click on shows
    - [ ] Implement progress visual for play button when download and when downloaded completely

- Caching
    - [X] Add in timeout for JSON data to be reloaded
    - [ ] Add in clean up for images that have not been accessed recently.

- Downloading
    - [X] Allow for progress of download
    - [X] Custom thread pool
    - [X] Allow for overflow of blocking queue (will do some more research)
    - [X] Move downloading specific audio files from the background service to a separate class to handle it
    - [ ] Allow for picking up where a download left off instead of complete redownload
    
- Search
    - [X] Add in search activity
    - [X] Clean up the way Search activity looks and works to be all in same activity
    - [ ] Add in option to search for more if a show/channel pops up

- Config
    - [X] Setup env vars for config or something like that
    
- Rating
    - Start - send update after 5 minutes https://dev.npr.org/guide/services/listening/#Ratings 
    - ThumbsUP - https://dev.npr.org/guide/services/listening/#Ratings
        - not super important but we should have it
        - optional update for the other one
    

# Bugs (for now this is fine... I know I should use a tracker, but I am only one developing rn)
- [X] Shows Dialog - playNow should disable add to queue and vice versa
- [X] Shows Dialog - addToQueue not triggering proper play and next buttons

# Thanks to:
https://www.iconfinder.com/icons/1054997/audio_radio_icon for the icon
# THIS IS NOT AFFILIATED TO NPR IN ANY CAPACITY. This is a community project and an avid fan that wants a better Android App

# Open Source NPR Community Android App

The purpose of this app is to make the Android experience for NPR cleaner and more user friendly
adding in queues as well as caching for images and files locally to remove the terrible buffering
that the NPR One app has. This is just some of problems with a few enhancements that I will be adding.

This repo needs a lot more love so please feel free to contribute. Below is a list of features
I have been working on and plan on working on. This app in its current form is not read for 
production.

## Using this app

You need to setup your own Auth2.0 Server. Lucky for you I already have done that if you go to
my other repo [here](https://github.com/OpenSourceNPRCommunity/oauth2_proxy) and set this up you should be good to go!

After that simply clone this repo and start messing around.

I use a Samsung, so that is my targeted platform.

## Supported Features
- NPR
    - Logging in to user account via OAUTH2
- Media Queue
    - Automatic caching and local storage of both Images and Audio
    - Changing order of items in queue (with dragging them up and down)
    - Adding Items to Queue
    - Removing items from queue
- Recommendations
    - Listing selected recommendation channels
    - Updating Recommendations as media ends and as it moves onto the next story
- Searching
    - Showing all searched options
    - Adding options to queue from Search
- Station
    - Ability to change stations
- Ratings
    - Send required ratings to NPR endpoint
- Android
    - Lock Screen compatible
    - Service runs in background handling downloading and long term running

## In the works: Some thins I would like to do and are planning on doing
__
Overall Goals:
- Refactoring a lot of the Code
- Testing - I need to add it 

Immediate Goals:
- Data Stopped and Started background
- Add time and date of podcast to queue and fragment info
- Check for internet and timeout, display errors for HTTP Calls
- Action button (button) and (action) for aggregation recommendations
- (More option) for more podcast
- Add previous list to reload
- Notifications listen for updates

### Long Term not as important goals, but have been on my mind...

- [X] Some type of Icon
- Pause Button Bluetooth

- Testing
    - Any sort of testing... got to get on that for some stability. I have been more interested 
    in developing features for now... I know that is not the best form of coding, but I will 
    eventually get onto testing. 

- Shows Fragment
    - [ ] Implement loading when click on shows
    - [ ] Implement progress visual for play button when download and when downloaded completely

- Downloading
    - [ ] Allow for picking up where a download left off instead of complete redownload
    
- Search
    - [ ] Add in option to search for more if a show/channel pops up
    
- Rating
    - Start - send update after 5 minutes https://dev.npr.org/guide/services/listening/#Ratings 

# Thanks to:
https://www.iconfinder.com/icons/1054997/audio_radio_icon for the icon

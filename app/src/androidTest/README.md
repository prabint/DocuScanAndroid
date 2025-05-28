# UI Test

## Emulator config.ini

UI tests are screenshot based which are recorded on `Pixel XL API 34`. Same device is required for
test to pass:

```
hw.lcd.density=560
hw.lcd.height=2560
hw.lcd.width=1440
hw.camera.back=virtualscene
```

## [Toren1BD.posters](files/Toren1BD.posters) and [custom.jpeg](files/custom.jpeg)

The `custom.jpeg` is our test image that contains a document picture. We position this image in
android emulator's
camera virtual scene such that when the camera launches, the camera is looking at this custom.jpeg
image.
This positioning is
done in `Toren1BD.posters`. In that file, the following lines were appended to the end:

```
poster custom
size 1.2 1.2
position 0.05 -0.15 -0.7
rotation 0 0 0
default custom.jpeg
```

These files must be copied over to `~/Library/Android/sdk/emulator/resources/`
and/or `~/.android/sdk/emulator/resources/`.

TODO: There's also `macros/Walk_to_image_room` which moves the camera automatically. But it's not
clear how to create a
macro and if we can run it from code.

## Golden value screenshots

These are the expected screenshots that are matched against the screenshots taken during the test.
We capture these
golden screenshots by running the test using `./gradlew cAT` which pulls and saves screenshots
captured (using `.writeToTestStorage()`)
into `build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/`. These
screenshots are copied
to `androidTest/assets` folder. This is one-time manual task but needs to be re-done if there is UI
change in the app.
When running the
test we take
screenshot (using `.captureToBitmap()`) and compare it against the images
inside `androidTest/assets`.

Note that we have to use custom `isSimilar()` method instead of the already
existing `Bitmap.sameAs(Bitmap)` because the
android emulator virtual scene is not steady. It mimics human hands and thus is shaky. The image
captured is not always
100% exactly same.
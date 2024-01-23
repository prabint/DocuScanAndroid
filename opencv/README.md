This module contains OpenCV android sdk code. Current version is 4.9.0.

To update opencv:

1. [Download](https://sourceforge.net/projects/opencvlibrary/files/) and extract latest opencv library for android.
2. Goto `OpenCV-android-sdk/sdk/java/src` and copy over the `org` folder into this module's `src/main/java/`. Old files
   can be backed up before proceeding.
3. Goto `OpenCV-android-sdk/sdk/native/libs` and copy `libopencv_java4.so` for each arch type into this
   module's `src/main/jniLibs/`
4. Goto `$HOME/Library/Android/sdk/ndk/xx.x.xxxxx/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib` and copy
   over `libc++_shared.so` for each arch type into this module's `src/main/jniLibs/`. #TODO Ideally this should have
   been resolved by adding the following to but it's not working:
    ```
    defaultConfig {
      externalNativeBuild {
        cmake {
           cppFlags += ""
           arguments += "-DANDROID_STL=c++_shared"
        }
      }
    }
   ```
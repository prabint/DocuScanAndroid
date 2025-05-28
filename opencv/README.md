This module contains OpenCV android sdk code. Current version is 4.11.0.

To update opencv:

1. [Download](https://sourceforge.net/projects/opencvlibrary/files/) and extract latest opencv
   library for android.
2. Goto `OpenCV-android-sdk/sdk/java/src` and copy over the `org` folder into this module's
   `src/main/java/`. Old files
   can be backed up before proceeding.
3. Goto `OpenCV-android-sdk/sdk/native/libs` and copy `libopencv_java4.so` for each arch type into
   this
   module's `src/main/jniLibs/`
4. Goto
   `$HOME/Library/Android/sdk/ndk/xx.x.xxxxx/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib`
   and copy
   over `libc++_shared.so` for each arch type into this module's `src/main/jniLibs/`:
   `{aarch64-linux-android -> arm64-v8a, arm-linux-androideabi -> armeabi-v7a, i686-linux-android ->
   x86, x86_64-linux-android -> x86_64}`. Otherwise, you will get this error:
   ```
   System.err                                      W  java.lang.UnsatisfiedLinkError: dlopen failed: library "libc++_shared.so" not found: needed by /data/app/~~ycgEXmQ4-yzcw6D_34LPaQ==/prabin.timsina.documentscanner.debug-UNp7IYcz3ZK5lNTJreMmMQ==/base.apk!/lib/arm64-v8a/libopencv_java4.so in namespace classloader-namespace
   System.err                                      W  	at org.opencv.android.StaticHelper.loadLibrary(StaticHelper.java:44)
   System.err                                      W  	at org.opencv.android.StaticHelper.initOpenCV(StaticHelper.java:19)
   System.err                                      W  	at org.opencv.android.OpenCVLoader.initLocal(OpenCVLoader.java:31)
   OpenCV/StaticHelper                             D  First attempt to load libs fails
   ```
   #TODO Ideally this should have
   been resolved by adding the following code but it's not working:
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
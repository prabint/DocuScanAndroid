package org.opencv.android;

import android.util.Log;

import org.opencv.core.Core;

class StaticHelper {

    private static final String TAG = "OpenCV/StaticHelper";

    public static boolean initOpenCV(boolean InitCuda) {
        boolean result;
        String libs = "";

        if (InitCuda)
            Log.w(TAG, "CUDA support was removed!");

        Log.d(TAG, "First attempt to load libs");
        if (loadLibrary("opencv_java4")) {
            Log.d(TAG, "First attempt to load libs is OK");
            String eol = System.getProperty("line.separator");
            for (String str : Core.getBuildInformation().split(eol))
                Log.i(TAG, str);

            result = true;
        } else {
            Log.d(TAG, "First attempt to load libs fails");
            result = false;
        }

        return result;
    }

    private static boolean loadLibrary(String Name) {
        boolean result = true;

        Log.d(TAG, "Trying to load library " + Name);
        try {
            System.loadLibrary(Name);
            Log.d(TAG, "Library " + Name + " loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "Cannot load library \"" + Name + "\"");
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    private static native String getLibraryList();
}

//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.objdetect;

// C++: class QRCodeDetectorAruco

public class QRCodeDetectorAruco extends GraphicalCodeDetector {

    protected QRCodeDetectorAruco(long addr) {
        super(addr);
    }

    public QRCodeDetectorAruco() {
        super(QRCodeDetectorAruco_0());
    }

    //
    // C++:   cv::QRCodeDetectorAruco::QRCodeDetectorAruco()
    //

    /**
     * QR code detector constructor for Aruco-based algorithm. See cv::QRCodeDetectorAruco::Params
     *
     * @param params automatically generated
     */
    public QRCodeDetectorAruco(QRCodeDetectorAruco_Params params) {
        super(QRCodeDetectorAruco_1(params.nativeObj));
    }


    //
    // C++:   cv::QRCodeDetectorAruco::QRCodeDetectorAruco(QRCodeDetectorAruco_Params params)
    //

    // internal usage only
    public static QRCodeDetectorAruco __fromPtr__(long addr) {
        return new QRCodeDetectorAruco(addr);
    }


    //
    // C++:  QRCodeDetectorAruco_Params cv::QRCodeDetectorAruco::getDetectorParameters()
    //

    // C++:   cv::QRCodeDetectorAruco::QRCodeDetectorAruco()
    private static native long QRCodeDetectorAruco_0();


    //
    // C++:  QRCodeDetectorAruco cv::QRCodeDetectorAruco::setDetectorParameters(QRCodeDetectorAruco_Params params)
    //

    // C++:   cv::QRCodeDetectorAruco::QRCodeDetectorAruco(QRCodeDetectorAruco_Params params)
    private static native long QRCodeDetectorAruco_1(long params_nativeObj);


    //
    // C++:  aruco_DetectorParameters cv::QRCodeDetectorAruco::getArucoParameters()
    //

    // Return type 'aruco_DetectorParameters' is not supported, skipping the function


    //
    // C++:  void cv::QRCodeDetectorAruco::setArucoParameters(aruco_DetectorParameters params)
    //

    // Unknown type 'aruco_DetectorParameters' (I), skipping the function

    // C++:  QRCodeDetectorAruco_Params cv::QRCodeDetectorAruco::getDetectorParameters()
    private static native long getDetectorParameters_0(long nativeObj);

    // C++:  QRCodeDetectorAruco cv::QRCodeDetectorAruco::setDetectorParameters(QRCodeDetectorAruco_Params params)
    private static native long setDetectorParameters_0(long nativeObj, long params_nativeObj);

    // native support for java finalize()
    private static native void delete(long nativeObj);

    /**
     * Detector parameters getter. See cv::QRCodeDetectorAruco::Params
     *
     * @return automatically generated
     */
    public QRCodeDetectorAruco_Params getDetectorParameters() {
        return new QRCodeDetectorAruco_Params(getDetectorParameters_0(nativeObj));
    }

    /**
     * Detector parameters setter. See cv::QRCodeDetectorAruco::Params
     *
     * @param params automatically generated
     * @return automatically generated
     */
    public QRCodeDetectorAruco setDetectorParameters(QRCodeDetectorAruco_Params params) {
        return new QRCodeDetectorAruco(setDetectorParameters_0(nativeObj, params.nativeObj));
    }

    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }

}

# DocuScanAndroid

This is a sample document scanner android project that uses OpenCV for edge-detection and
perspective transformation of images with document. The core logic that utilizes OpenCV and does all
the math is used
from [adityaarora1/LiveEdgeDetection](https://github.com/adityaarora1/LiveEdgeDetection) and
some
from [zynkware/Document-Scanning-Android-SDK](https://github.com/zynkware/Document-Scanning-Android-SDK/tree/master).
This sample project uses jetpack compose, cameraX and latest core android libraries.

## Demo

1. Uses live camera feed to show a rectangular box on top of detected document
2. Polygon view to manually edit edges
3. Image preview after applying perspective transformation
4. Perspective transformation on image selected from gallery

![demo.gif](./assets/demo.gif)

## License

```
MIT License

Copyright (c) [2024] [Prabin Timsina]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
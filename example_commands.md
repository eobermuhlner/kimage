
```
kimage 'inputImage.medianFilter(5)' images/animal.png

kimage 'inputImage.gaussianBlurFilter(5)' images/animal.png

kimage 'inputImage[Channel.Red].average()' images/animal.png

kimage 'inputImage[Channel.Luminance].stddev()' images/animal.png

```
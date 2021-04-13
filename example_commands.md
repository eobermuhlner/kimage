
```
kimage --execute 'inputImage.medianFilter(5)' images/animal.png

kimage --execute 'inputImage[Channel.Red].avg()' images/animal.png

kimage --execute 'inputImage[Channel.Luminance].stddev()' images/animal.png

kimage --verbose --script scripts/remove_background_using_median_and_blur.kts images/animal.png

kimage --verbose --script scripts/align_average.kts --param checkRadius=10 --param searchRadius=20 images/lena512color.tiff images/lena512color_gimp_median3.tiff

```
# kimage
Image processing using kotlin scripts.



# Scripts

The following scripts are distributed together with the `kimage` application.

---

## Script: align

    kimage [OPTIONS] align
        [--arg checkRadius=INT]
        [--arg searchRadius=INT]
        [--arg centerX=INT]
        [--arg centerY=INT]
        [--arg errorThreshold=DOUBLE]
        [--arg prefix=STRING]
        [--arg saveBad=BOOLEAN]
        [--arg prefixBad=STRING]
        [FILES]

Align multiple images.
The base image is the first image argument.
The remaining image arguments are aligned to the base image by searching for a matching feature.

The feature to match is defined by the centerX/centerY coordinates in the base image and the check radius.
The searchRadius defines how far the matching feature is searched.

Use the --debug option to save intermediate images for manual analysis.


### Argument: checkRadius

- Type: int
- Minimum value: 0


The radius to check for similarity.
The default value is calculated from the base image.


### Argument: searchRadius

- Type: int
- Minimum value: 0


The search radius defining the maximum offset to align.
The default value is calculated from the base image.


### Argument: centerX

- Type: int
- Minimum value: 0


The X coordinate of the center to check for alignment.
The default value is calculated from the base image.


### Argument: centerY

- Type: int
- Minimum value: 0


The Y coordinate of the center to check for alignment.
The default value is calculated from the base image.


### Argument: errorThreshold

- Type: double
- Minimum value: 0.0
- Default value: 0.001


The maximum error threshold for storing an aligned image.
Images with an error above the error threshold will be either ignored
or saved with a different prefix.
See `saveBad`, `prefixBad`.


### Argument: prefix

- Type: string
- Default value: `aligned`

The prefix of the aligned output files.

### Argument: saveBad

- Type: boolean

Controls whether badly aligned images are saved.

### Argument: prefixBad

- Type: string
- Default value: `badaligned`

The prefix of the badly aligned output files.


---

## Script: calibrate

    kimage [OPTIONS] calibrate
        [--arg bias=IMAGE]
        [--arg dark=IMAGE]
        [--arg flat=IMAGE]
        [--arg darkflat=IMAGE]
        [FILES]

Calibrates bias/dark/flat/darkflat/light images.


### Argument: bias

- Type: image

### Argument: dark

- Type: image

### Argument: flat

- Type: image

### Argument: darkflat

- Type: image


---

## Script: color-stretch

    kimage [OPTIONS] color-stretch
        [--arg brightness=DOUBLE]
        [--arg curve=STRING]
        [--arg custom1X=DOUBLE]
        [--arg custom1Y=DOUBLE]
        [--arg custom2X=DOUBLE]
        [--arg custom2Y=DOUBLE]
        [FILES]

Stretches the colors of an image to fill the entire value range.


### Argument: brightness

- Type: double
- Minimum value: 0.0
- Default value: 2.0


The power value of the brightness increase.

- A power value > 1 increases the brightness.
- A power value = 0 does not change the brightness.
- A power value < 1 increases the brightness.


### Argument: curve

- Type: string
- Allowed values:
  - `linear`
  - `s-curve`
  - `s-curve-bright`
  - `s-curve-dark`
  - `s-curve-strong`
  - `s-curve-super-strong`
  - `bright+`
  - `dark+`
  - `bright-`
  - `dark-`
  - `custom1`
  - `custom2`
  - `all`
- Default value: `s-curve`


The curve shape used to modify the contrast.


### Argument: custom1X

- Type: double
- Default value: 0.2

### Argument: custom1Y

- Type: double
- Default value: 0.1

### Argument: custom2X

- Type: double
- Default value: 0.8

### Argument: custom2Y

- Type: double
- Default value: 0.9


---

## Script: convert

    kimage [OPTIONS] convert
        [FILES]

Converts the image into another format.



---

## Script: delta

    kimage [OPTIONS] delta
        [--arg factor=DOUBLE]
        [--arg channel=STRING]
        [FILES]

Creates delta images between the first image and all other images.

The output images show the pixel-wise difference between two images on a specific channel (default is Luminance).
The difference is color coded:
- black = no difference
- blue  = pixel in the first image is brighter
- red   = pixel in the first image is darker

The `factor` argument controls how much the differences are exaggerated.

This script is a useful to compare images, especially outputs of other scripts with different arguments.


### Argument: factor

- Type: double
- Default value: 5.0


Controls how much the differences are exaggerated.


### Argument: channel

- Type: string
- Allowed values:
  - `Red`
  - `Green`
  - `Blue`
  - `Luminance`
  - `Gray`
- Default value: `Luminance`


The channel used to calculate the difference between two images.



---

## Script: hdr

    kimage [OPTIONS] hdr
        [--arg saturationBlurRadius=INT]
        [--arg contrastWeight=DOUBLE]
        [--arg saturationWeight=DOUBLE]
        [--arg exposureWeight=DOUBLE]
        [FILES]

Stacks multiple images with different exposures into a single HDR image.


### Argument: saturationBlurRadius

- Type: int
- Default value: 3

### Argument: contrastWeight

- Type: double
- Default value: 0.2

### Argument: saturationWeight

- Type: double
- Default value: 0.1

### Argument: exposureWeight

- Type: double
- Default value: 1.0


---

## Script: histogram

    kimage [OPTIONS] histogram
        [--arg width=INT]
        [--arg height=INT]
        [FILES]

Creates a histogram image.


### Argument: width

- Type: int
- Default value: 512


The width of the histogram.


### Argument: height

- Type: int
- Default value: 300


The height of the histogram.



---

## Script: info

    kimage [OPTIONS] info
        [FILES]

Print info about images.



---

## Script: remove-background-gradient

    kimage [OPTIONS] remove-background-gradient
        [--arg removePercent=DOUBLE]
        [--arg gridSize=INT]
        [--arg kappa=DOUBLE]
        [FILES]

Removes the background from the input image by subtracting a gradient calculated from the color of fix points.

This script is useful for astrophotography if the fix points are chosen to represent the background.

Use the --debug option to save intermediate images for manual analysis.


### Argument: removePercent

- Type: double
- Default value: 99.0


The percentage of the calculated background that will be removed.


### Argument: gridSize

- Type: int
- Default value: 5


The size of the grid in the x and y axis.
The number of grid points is the square of the `gridSize`.


### Argument: kappa

- Type: double
- Default value: 0.5


The kappa factor is used in sigma-clipping of the grid to ignore grid points that do not contain enough background.



---

## Script: remove-background-median

    kimage [OPTIONS] remove-background-median
        [--arg removePercent=DOUBLE]
        [--arg medianFilterPercent=DOUBLE]
        [--arg blurFilterPercent=DOUBLE]
        [--arg medianFilterSize=INT]
        [--arg blurFilterSize=INT]
        [FILES]

Removes the background from the input image by subtracting a blurred median of the input.

This script is useful for astrophotography if the image contains mainly stars and not too much nebulas.
The size of the median filter can be increased to remove stars and nebulas completely.

Use the --debug option to save intermediate images for manual analysis.


### Argument: removePercent

- Type: double
- Default value: 99.0


The percentage of the calculated background that will be removed.


### Argument: medianFilterPercent

- Type: double
- Default value: 0.0


The size of the median filter in percent of the image size.


### Argument: blurFilterPercent

- Type: double
- Default value: 0.0


The size of the blur filter in percent of the image size.


### Argument: medianFilterSize

- Type: int
- Default value: 0


The size of the median filter in pixels.
If this value is 0 then the `medianFilterPercent` is used to calculate it.
If the `medianFilterPercent` is 0.0 then the median filter size is calculated automatically from the image size.


### Argument: blurFilterSize

- Type: int
- Default value: 0


The size of the blur filter in pixels.
If this value is 0 then the `blurFilterPercent` is used to calculate it.
If the `blurFilterPercent` is 0.0 then the blur filter size is calculated automatically from the image size.



---

## Script: stack

    kimage [OPTIONS] stack
        [--arg method=STRING]
        [--arg kappa=DOUBLE]
        [--arg iterations=INT]
        [FILES]

Stacks multiple image using one of several algorithms.


### Argument: method

- Type: string
- Allowed values:
  - `median`
  - `average`
  - `max`
  - `min`
  - `sigma-clip-median`
  - `sigma-clip-average`
  - `sigma-winsorize-median`
  - `sigma-winsorize-average`
  - `winsorized-sigma-clip-median`
  - `winsorized-sigma-clip-average`
  - `all`
- Default value: `sigma-clip-median`


Method used to calculate the stacked image.

The method `sigma-clip-median` removes outliers before using `median` on the remaining values.
The method `sigma-clip-average` removes outliers before using `average` on the remaining values.
The method `sigma-winsorize-median` replaces outliers with the nearest value in sigma range before using `median`.
The method `sigma-winsorize-average` replaces outliers with the nearest value in sigma range before using `average`.
The method `winsorized-sigma-clip-median` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `median`.
The method `winsorized-sigma-clip-average` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `average`.

All methods that use sigma-clipping print a histogram with the information how many input values where actually used to stack each output value.


### Argument: kappa

- Type: double
- Minimum value: 0.0
- Default value: 2.0


The kappa factor is used in sigma-clipping to define how far from the center the outliers are allowed to be.


### Argument: iterations

- Type: int
- Minimum value: 0
- Default value: 10


The number of iterations used in sigma-clipping to remove outliers.



---

## Script: stack-average

    kimage [OPTIONS] stack-average
        [FILES]

Stacks multiple images by calculating a pixel-wise average.

This stacking script is useful if there are no outliers.



---

## Script: stack-max

    kimage [OPTIONS] stack-max
        [FILES]

Stacks multiple images by calculating a pixel-wise maximum.

This stacking script is useful to find outliers and badly aligned images.



---

## Script: test-multi

    kimage [OPTIONS] test-multi
        [--arg intArg=INT]
        [--arg optionalIntArg=INT]
        [--arg doubleArg=DOUBLE]
        [--arg booleanArg=BOOLEAN]
        [--arg stringArg=STRING]
        [--arg allowedStringArg=STRING]
        [--arg regexStringArg=STRING]
        [FILES]

Test script to show how to handle multiple images in a kimage script.


### Argument: intArg

- Type: int
- Minimum value: 0
- Maximum value: 100
- Default value: 0

Example argument for an int value.

### Argument: optionalIntArg

- Type: int
- Minimum value: 0
- Maximum value: 100

Example argument for an optional int value.

### Argument: doubleArg

- Type: double
- Minimum value: 0.0
- Maximum value: 100.0
- Default value: 50.0

Example argument for a double value.

### Argument: booleanArg

- Type: boolean

Example argument for a boolean value.

### Argument: stringArg

- Type: string
- Default value: `undefined`

Example argument for a string value.

### Argument: allowedStringArg

- Type: string
- Allowed values:
  - `red`
  - `green`
  - `blue`
- Default value: `red`

Example argument for a string value with some allowed strings.

### Argument: regexStringArg

- Type: string
- Must match regular expression: `a+`
- Default value: `aaa`

Example argument for a string value with regular expression.


---

## Script: test-single

    kimage [OPTIONS] test-single
        [--arg intArg=INT]
        [--arg optionalIntArg=INT]
        [--arg doubleArg=DOUBLE]
        [--arg booleanArg=BOOLEAN]
        [--arg stringArg=STRING]
        [--arg allowedStringArg=STRING]
        [--arg regexStringArg=STRING]
        [FILES]

Test script to show how to handle single images in a kimage script.


### Argument: intArg

- Type: int
- Minimum value: 0
- Maximum value: 100
- Default value: 0

Example argument for an int value.

### Argument: optionalIntArg

- Type: int
- Minimum value: 0
- Maximum value: 100

Example argument for an optional int value.

### Argument: doubleArg

- Type: double
- Minimum value: 0.0
- Maximum value: 100.0
- Default value: 50.0

Example argument for a double value.

### Argument: booleanArg

- Type: boolean

Example argument for a boolean value.

### Argument: stringArg

- Type: string
- Default value: `undefined`

Example argument for a string value.

### Argument: allowedStringArg

- Type: string
- Allowed values:
  - `red`
  - `green`
  - `blue`
- Default value: `red`

Example argument for a string value with some allowed strings.

### Argument: regexStringArg

- Type: string
- Must match regular expression: `a+`
- Default value: `aaa`

Example argument for a string value with regular expression.



# Known problems in Python versions / Improvements



## Square area calculation

Calculation of square area is needlessly complex and gives a small numerical error



```
def calc_area_of_square(nr_of_squares_in_row):
    micrometer_per_pixel = 0.1602804  # Referenced from Fiji
    pixel_per_image = 512  # Referenced from Fiji
    micrometer_per_image = micrometer_per_pixel * pixel_per_image
    micrometer_per_square = micrometer_per_image / nr_of_squares_in_row
    area = micrometer_per_square * micrometer_per_square
    return area
```



Better is:



```
public static final double PIXEL_WIDTH          = 0.1603251;                          
public static final double PIXEL_HEIGHT         = 0.1603251;                           
public static final int    NUMBER_PIXELS_WIDTH  = 512;                                 
public static final int    NUMBER_PIXELS_HEIGHT = 512;                                 
public static final double IMAGE_WIDTH          = PIXEL_WIDTH * NUMBER_PIXELS_WIDTH;   

public static double calculateSquareArea(int nrSquares) {
    return IMAGE_WIDTH * IMAGE_HEIGHT / nrSquares;
}
```



## Assigning tracks to squares

In assigning tracks to squares there is a little error that causes some tracks to be assigned to two squares. The total number of tracks assigned to squares is larger than the number of tracks in the recording.  Has been fixed.



## Determining the visibility of Squares

The function `select_squares_with_parameters` is called twice. 

The first time to determine for individual squares the visibility, with `0nly_valid_tau`  set to True.

The routine is called a second time to determine which squares should be included for the Recording Tau calculation. This time the  `0nly_valid_tau` parameter is set is (correctly) set to False. Unfortunately the `Visible` flag of All Squares is reset in this second operation and the initial correct outcome is overwritten.

The easiest fix would be to after the recording Tau has been calculated to call `select_squares_with_parameters` again with `0nly_valid_tau` set to True.

Look for `select_squares_with_parameters` in `Generete_Squares.py`



## Curve Fitting

The Python implementations has been replaced by a morev sophisticated Java implementation. Not to address a known problem, but it may give differences, i.e. being able to curvefit wjerevthta prefioslybfailed, or the otgher way around.



## Background calculation

In Python a very simple algorithm was implemented, that took the average of the 10 or 20 least populated squares (ignoring squares that had no track at all).

In Java a mathematical split is made between a group of squares with low and high content. Becauxe now the emnpty squares are included the average background density ios a lot lower and the Density Ratio a lot higher than before, 


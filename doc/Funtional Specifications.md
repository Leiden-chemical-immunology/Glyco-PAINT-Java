# Functional description of the Trackmate plugin



The plugin is started, after starting Fiji, by selecting from the Plugins menu, Glyco-PAINT and Run. A dialog box like shown below pops up.

![trackmate-plugin](/Users/hans/JavaPaintProjects/doc/Pictures/trackmate-plugin.png)

Also  visible is the Paint Console , that is used to provide feedback to users on the status and progress of operations.



![paint-console-initial](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-initial.png)  

## Overview

On top, the currently selected Project and Images Roots are shown, the Browse buttons on the right allow users to change that selection. 

Immediately below, users can select to have squares generated after TrackMate processing is completed. If the checkbox is ticked, the parameters can be entered.

Below that, a list of Experiments is displayed. TrackMate (and Generate Squares)  calculations are performed on selected Experiments only. The 'Select All' and 'Clear All' buttons facilitate easy selection of Experiments.

At the bottom of the dialog, three checkboxes are shown.

- If the Save Experiments option is clicked, the current selection of Experiments will be saved.
- The Verbose option, when clicked, provides more detailed feedback to the user.
- The Sweep option, used only in specific cases, performs a parameter sweep. In this process, TrackMate calculations are performed with varying parameters. For more information on this option, please refer to ….

Pressing the Ok button starts the calculation. Pressing the Cancel button when a calculation is running stops it. When no calculations are running, pressing Cancel wil close the dialog.



## Experiments

The experiments that are shown, are subdirectories directly under the selected Project Root, provided that the contain an 'Experiment Info'.  If the user selects a different Project Root, the display of Experiments is refreshed.

​	Note: If 'Sweep' calculations have been performed, there will be a Sweep directory in the Project Root 	which will contain an 'Experiment Info' file,  but it will never be shown here.

A previously saved selection of 'Experiments' is stored in the 'Paint Configuration' file that is present in every 'Project Root'. This file will be automatically created if it does not exist. If for some reason it is corrupted, you can safely delete it.



## Run TrackMate

#### The overall flow

When Roots have been set and Experiments selected, pressing the 'Ok' button starts the operation. The 'Ok' button changes to 'Running...' for as long as the operation runs. Only the 'Cancel' button is enabled during this period and can be used to interrupt the calculation.

If the operations completes normally, the 'Running...' button reverts back to 'OK', but remains disabled. Only after changing the  Experiment selection, the 'OK' button becomes active again (to prevent the user accidentally restarting a potentially long calculation).

If the user cancels the operation, the 'Running...' button changes to 'Cancelled' during the cleanup and then reverts back to 'Ok'.

#### Validation

The first step is that the validity of the 'Experiment Info' files in the selected Experiments is verified.  An error is raised if the colums read in are different from expected, or when values in colums are inconsistent with the expected datatype (for example when a numeric value is expected and a text string is present).  A more subtle error is detected when there differences in attributes of replicates of the same condition. 

#### TrackMate Parameters

The TrackMate calculations are influences by a set of parameters, that do normally not require changing and which are specified through the Paint Configuration file.  If 'Verbose' is set, the parameters are displayed in the Paint Console. See below.

![paint-console-running](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-running.png) 

Two parameters, specifying the maximum numbers of spots in an image and the maximum number of seconds per image, limit the processing time. Experience has shown that when the number of spots in an image exceeds 2,000,000, the tracking takes very long and rarely delivers meaningful results. Setting a higher value for 'Threshold' for that image may be required. There is also a limit on the number of processing seconds per image. The two, in combination, prevent the pipeline choking up.  

In contrast to the general TrackMate parameters, the 'Threshold' parameter needs to be set for every recording separately and therefore is specified in the 'Experiment Info' file.

#### TrackMate Results

Upon completion of a TrackMate run, two new files have been created in each 'Experiment' directory: 'Tracks' and 'Recordings'.
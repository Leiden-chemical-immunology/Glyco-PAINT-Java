# Functional description of the Trackmate plugin



The Glyco-PAINT plugin is started, after starting Fiji, by selecting from the Plugins menu, Glyco-PAINT and Run. A dialog box like the one shown below pops up.

![trackmate-plugin](/Users/hans/JavaPaintProjects/doc/Pictures/trackmate-plugin.png)

A Paint Console, that is used to provide feedback to users on the status and progress of operations, is also opened.

![paint-console-initial](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-initial.png)  

## The plugin Userinterface

On top, the currently selected Project and Images Roots are shown; the Browse buttons on the right allow users to change that selection.

Immediately below, users can select to have squares generated after TrackMate processing is completed. If the checkbox is ticked, the parameters can be entered.

Below that, a list of Experiments is displayed. TrackMate (and Generate Squares)  calculations are performed on selected Experiments only. The 'Select All' and 'Clear All' buttons facilitate easy selection of Experiments.

At the bottom of the dialog, three checkboxes are shown.

- If the Save Experiments option is clicked, the current selection of Experiments will be saved.
- The Verbose option, when clicked, provides more detailed feedback to the user.
- The Sweep option, used only in specific cases, performs a parameter sweep. In this process, TrackMate calculations are performed with varying parameters. For more information on this option, please refer to ….

Pressing the Ok button starts the calculation. Pressing the Cancel button when a calculation is running stops it. When no calculations are running, pressing Cancel wil close the dialog.



## Experiments

The experiments that are shown, are subdirectories directly under the selected Project Root, provided that the contain an 'Experiment Info' file.  If the user selects a different Project Root, the display of Experiments is refreshed.

​	Note: If 'Sweep' calculations have been performed, there will be a Sweep directory in the Project Root 	which will contain an 'Experiment Info' file,  but it will never be shown here.

A previously saved selection of 'Experiments' is stored in the 'Paint Configuration' file that is present in every 'Project Root'. This file will be automatically created if it does not exist. If for some reason it is corrupted, you can safely delete it.



## Run TrackMate

#### The overall flow

When Roots have been set and Experiments selected, pressing the 'Ok' button starts the operation. The 'Ok' button changes to 'Running...' for as long as the operation runs. Only the 'Cancel' button is enabled during this period and can be used to interrupt the calculation.

If the operations completes normally, the 'Running...' button reverts back to 'OK', but remains disabled. Only after changing the  Experiment selection, the 'OK' button becomes active again (to prevent the user accidentally restarting a potentially long calculation).

If the user cancels the operation, the 'Running...' button changes to 'Cancelled' during the cleanup and then reverts back to 'Ok'.

#### Validation

The first step is that the validity of the 'Experiment Info' files in the selected Experiments is verified.  An error is raised if the colums read in are different from expected, or when values in colums are inconsistent with the expected datatype (for example when a numeric value is expected and a text string is present).  A more subtle error is detected when there differences in attributes of replicates of the same condition. 

#### TrackMate input parameters

The TrackMate calculations are influenced by a set of parameters, that do normally not require changing and which are specified in the 'Paint Configuration' file.  If 'Verbose' is set, the parameters are displayed in the 'Paint Console'. See below.

![paint-console-running](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-running.png) 

Two parameters, specifying the maximum numbers of spots in an image and the maximum number of seconds per image, limit the processing time. Experience has shown that when the number of spots in an image exceeds 2,000,000, the tracking takes long and rarely delivers meaningful results. Setting a higher value for 'Threshold' (in the 'Experiment Info' file) for that image may be required. Also a limit on the number of processing seconds per image can be set. The two, in combination, prevent the pipeline choking up.  

In contrast to the general TrackMate parameters, the 'Threshold' parameter needs to be set for every recording separately and therefore is specified in the 'Experiment Info' file.



#### Recordings processed

The 'Experiment Info' files in the selected Experiments specify which Recordings are to be processed. An example of an 'Experiment Info' file is shown below. Recordings for which the 'Process Flag' is set to 'TRUE' will be processed. Allowable 'TRUE' values are TRUE, 1, Yes, Y and T; valid 'FALSE' calues are FALSE, 0, No, N and F, all case insensitive. Other values will trigger an error.

For each Recording a 'Threshold' value is specified. A low value for Threshold will cause many features to be recognised as spots; high values will lead to fewer spots. The optimal value depends on the experiment and the user likely will need to resort to an iterative process to find the optimal values. To avoid long processing times,  initially high 'Threshold' values should be chosen      

![experiment-info](/Users/hans/JavaPaintProjects/doc/Pictures/experiment-info.png)

#### Paint Configuration file

The Paint Configuration file is a readable file in JSON format and contains 'Project' specific attributes. The file is read and written by software, but can also be inspected and edited with any normal plain text editor (Visual Studio Code, BBedit, Sublime). Below is the Paint Config file shown in Visual Code Studio.

In general users will not often have to edit the Paint Configuration and should only do so when they can assess the consequences of changes.  For information on the values in the TrackMate section, please refer to the [Track Mate manual](https://imagej.net/media/plugins/trackmate/trackmate-manual.pdf).

The file is 'self-healing', e.g., when necessary atrributes cannot be found, they are recreated with sensible defaults.  If the whole file is missing, for example for a fresh 'Project Root' it will be regenerated from scratch.

![paint-config-json](/Users/hans/JavaPaintProjects/doc/Pictures/paint-config-json.png)Only Project specific values are stored in the Paint Configuration file.  For system wide settings, the macOS Preferences mechanism is used to store system wide attrbutes in  ~/Library/preferences/Glyco-PAINT.plist (for Windows an equivalent implementation is provided). Users will generally not interact directly with this file,  but it is here where the current 'Project Root' and 'Images Root' are kept.  Below the Glyco-PAINT.plist is shown with Xcode's  plist viewer.

![preferences](/Users/hans/JavaPaintProjects/doc/Pictures/preferences.png)

#### TrackMate Results

Upon completion of a TrackMate run, two new files have been created in each 'Experiment' directory: 'Tracks' and 'Recordings'. The Tracks file contains the tracks for all the recordings in the Experiment, with for each caclulated attributes (the individual spots of the tracks are not saved).  The Recordings file is an evolution of Experiment Info file with spots, track and runtime information added:

- Number of Spots - the total number of spots in the Recording.
- Number of Tracks - the total number of tracks that were identified.
- Number of Spots in All Tracks - the sum of all the spots in those tracks (some spots are not assigned to tracks and this nunber is alweays lower than the Number of Spots). 
- Number of Frames - the number of frames in the recording (will normally be 2000).
- Run Time - the time TrackMate took to process the Recording.
- Time Stamp - for references purpose.

In addition to the creating of the 'Tracks' file and the updating of 'Recordings', two directories with images are created. The 'TrackMate Images' directory contains  the processed images in which the tracks are shown.  The images in the TrackMate and Brightfield directories are used in the Viewer application.

![trackmate-image](/Users/hans/JavaPaintProjects/doc/Pictures/trackmate-image.png) 

In the 'Brightfield Images' directory the corresponding Brightfield images are collected.

![brightfield-image](/Users/hans/JavaPaintProjects/doc/Pictures/brightfield-image.png) 

#### The Paint Console

Each application of the Glyco-PAINT pipeline opens its own Paint Cnsole. In the totel of the window is listed what application the Paint Console is attached to.

More information is logged in the Paint Console than can be seen at any one time and therefore scrolling is provided. Normally the end of the log is shown, so that new information is visible. The 'Scroll Lock' checkbox (left bottom) stops the auto scroll mechanism to allow the user to inspect earlier parts of the log undisturbed.

The 'Highlight Problems' button jumps to [ERROR] and [WARN ] lines in the log to alert the user on irregularities. Repeatedly clicking the button will jump to the next problem. 

A 'Save' button allows the saving of the information in the console window. Generally this is not needed as most information is saved automatically to a log file (see  next section).



#### Paint log files 

Every time the Glyco-PAINT Fiji plugin (or any other component of the pipeline) is started, a log file is opened in the 'Logs' directory under the 'Project Root' directory. Log files are named after the pipeline component that is logging. Files are sequentially number, so that older information is not overwritten. In a heavily analysed 'Project Root', the Logs directory may accumukate a large number of log files and cleaning up occasionaly may be warranted.   



#### Generate Squares

If the 'Generate Squares' checkbox was ticked before the the calculation was started, squares will be generated for all the Recordings in the selected Experiments.   

The results are stored in Experiment-level 'Squares' files



Following the TrackMate processing, for each Recording a statistical procedure determines the background density, tye number of tracks per square  BackgroundEstimationResult estimateBackgroundDensit 

- Number of Tracks in Background 
- Number of Squares in Background
- Average Tracks in Background




Out directory

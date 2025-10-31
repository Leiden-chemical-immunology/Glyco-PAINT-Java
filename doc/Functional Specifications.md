



------

# Glyco-PAINT Pipeline 

------





# Introduction

This Java version of Glyco-PAINT will fully replace the current Python-based version. Functionally the versions are identical, but many improvements have been implemented. The most important improvements include:

- **Simplified user interface**. Fewer pipeline components, directly accessible as desktop applications (eliminating the need to use a development environment such as PyCharm as previously).
- **Improved validation**, making it less likely for incorrect input to interfere or invalidate with calculations.
- **Streamlined code**, recreated from the base, making it much easier to understand and maintain.
- **Better documentation**, making the code more accesible to users.
- **Improved version management**, fuly utilising the GutHub release mechanism and installers for macOS and Windows. 

Whereas the functionality of the this version is identical to the Pyhon version, results from the Python version are not 100% reproducible. Partially this is because of some improvements in implementation of core algorithms, but more so because of inherent indeterministic behaviour of the TrackMate calculation engine itself (also with the Python environment itself, results are not 100% reproducible). Differences are small and do not impact the analysis and conclusion drawn from experiments.

The code for the Python version and the new Java version is available on: [Glyco-PAINT Python](https://github.com/Leiden-chemical-immunology/Glyco-PAINT) Github and [Glyco-PAINT Java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java). You no longer need access to the code for running the pipeline, but nevertheless familiarity with the core algorithms is desirable for more effective use of the pipeline  

In addition to the published code itself, a  Javadoc site provides a complete, web-based reference of all public classes and methods in the PAINT software. It is automatically generated from the code comments and published on GitHub Pages, allowing you to browse the documentation directly in your web browser. Each module and package includes detailed descriptions of its purpose, parameters, and return values, making it easier to understand and extend the software. You can access the latest version of the Javadoc at [Glyco-PAINT Javadoc](https://leiden-chemical-immunology.github.io/Glyco-PAINT-Java/).

Example images are published on Zenodo and can be used as  [Glyco-PAINT reference images](https://doi.org/10.5281/zenodo.17487086).

Explanation on how to use the pipeline is provided as a working Markdown document, distributed via Githun or as a pdf file.   



# System requirements

The Glyco-PAINT pipeline us developed, tested and heavily used on macOS, but should run on Windows and Linux also. For the latter two platforms support however is at a reduced level, because of the absence of a good testing environment. 

Glyco-PAINTis a computationally heavy application, so you will enjoy a powerful computer. All development and analysis for the Glyco-PAINT paper has been done on a MacBook Pro M3 Max with 36GB memory, but on a less powerful configuration it will also run, but slower.

You need to have FiJi installed. If you don't you can download it from the [FiJi downloads](https://imagej.net/software/fiji/downloads) site.  TrackMate normally comes automatically with a FiJi installation. Information on [TrackMate](https://imagej.net/plugins/trackmate/) is available and you can also download it from there if needed.

The pipeline depends on the presence of the Java 8 JRE (Java Runtime Environment). Chances are that that is available on yout computer already. You can check on macOS from a terminal with:

```
/usr/libexec/java_home -V
```

On windows use Explorer to check the directories:

```
C:\Program Files\Java\
C:\Program Files (x86) Java\
```

If the Java 8 JRE is not present, you can download it from [Adoptium Java 8 JRE](https://adoptium.net/en-GB/temurin/releases?version=8&os=any&arch=any) (make sure you select JRE and not JDK).

  

# Pipeline components

The pipeline consist of a Fiji plugin that performs the TrackMate calculations and a number of separate apps. The two must important apps are:

- **Generate Squares**, which allows you to generate squares over the processed TrackMate images and calculates the square attributes such as Tau, Density, etc.
- **Viewer**, which allows you to visually inspect the TrackMate results in comparison with the brightfield image. You can change square selection parameters to determine which squares are included in subsequent analysis.

The latest version of the plugin integrates the Generate Squares functionality, so the Generate Squares may be retired over time.

In addition to these main apps there are two simple utilities:

- **Get Omero** which allows you to organise downloaded Omero files without extensive manual intervention. This addresses a peculiarity in Omero and is not part of the Glyco-PAINT pipeline.

- **Create Experiments** which helps you create an Experiment Info file that contains the instructions necessary to run the pipeline. The app does not do anything that a simple text based editor cannot also do, but you may find it convenient.         

  

# Installing the pipeline

There are multiple ways to install the Glyco-PAINT pipeline. The easiest is through a self extracting installer, likely called something like: Glyco-PAINT-Installer.sh.

For an easy installation route, agree with the developers at Leiden LIC access to the OneDrive Glyco-PAINT distribution site.  Download the installer file (typically with a 500 MB size) to any folder on your compurer (typically your ~/Downloads folder). Open a terminal and execute the following commands

```
cd ~/Downloads
./Glyco-PAINT-installer.sh
```

The installer will create a directory Glyco-PAINT in the ~/Applications directory and copy the four apps (Generate Squares, Viewer, Get Omero and Create Expriment) to it and will subsequentl attempt to copy the plugin jar to the Fiji plugins directory. 

The jar name will be similar to  **paint-fiji-plugin-0.0.3-SNAPSHOT-jar-with-dependencies.jar**. The number sequence 0.0.3 will be incremented with subsequent releases. The **SNAPSHOT** may be present,  indicating  this is a development version, or not, indicating a formal release version.

The Glyco-Paint apps can be started by simply clicking on them in the ~/Applications/Glyco-PAINT folder. There is no good reason for it but you can start the Glyco-PAINT apps also from the command line. For that you need to open the App bundles and locate the jar files in the Contents/Java directory. As example for Get Omero.

```
cd ~/Applications/Glyco-PAINT/Get Omero/Contents/Java
java -jar paint-get-omero-0.0.3-SNAPSHOT-jar-with-dependencies.jar
```



# The TrackMate plugin

The Glyco-PAINT plugin is started, after starting Fiji, by selecting from the Plugins menu, Glyco-PAINT and Run. A dialog box like the one shown below pops up.

![trackmate-plugin](/Users/hans/JavaPaintProjects/doc/Pictures/trackmate-plugin.png)

A Paint Console, that is used to provide feedback to users on the status and progress of operations, is also opened.

![paint-console-initial](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-initial.png)  

## The plugin user interface

On top, the currently selected **Project Root** and **Images Root** are shown; the Browse buttons on the right allow users to change that selection.

Immediately below, users can select to have squares generated after TrackMate processing is completed. If the checkbox is ticked, the parameters can be entered.

Below that, a list of **Experiments** is displayed. TrackMate (and Generate Squares)  calculations are performed on selected Experiments only. The 'Select All' and 'Clear All' buttons facilitate easy selection of Experiments.

At the bottom of the dialog, three checkboxes are shown.

- If the Save Experiments option is clicked, the current selection of Experiments will be saved.
- The Verbose option, when clicked, provides more detailed feedback to the user.
- The Sweep option, used only in specific cases, performs a parameter sweep. In this process, TrackMate calculations are performed with varying parameters. For more information on this option, please refer to ….

Pressing the Ok button starts the calculation. Pressing the Cancel button when a calculation is running stops it. When no calculations are running, pressing Cancel wil close the dialog.



## Experiments

The experiments that are shown, are subdirectories directly under the selected Project Root. To be shown as Experiment the directory needs to contain an **Experiment Info** file.  If the user selects a different Project Root, the display of Experiments is refreshed.

​	Note: If 'Sweep' calculations have been performed, there will be a Sweep directory in the Project Root 	which will contain an 'Experiment Info' file,  but it will never be shown here.

A previously saved selection of 'Experiments' is stored in the **Paint Configuration** file that is present in every Project Root. This file will be automatically created if it does not exist. If for some reason it is corrupted, you can safely delete it.



## Run TrackMate

#### The overall flow

When Roots have been set and Experiments selected, pressing the 'Ok' button starts the operation. The 'Ok' button changes to 'Running...' for as long as the operation runs. Only the 'Cancel' button is enabled during this period and can be used to interrupt the calculation.

If the operations completes normally, the 'Running...' button reverts back to 'OK', but remains disabled. Only after changing the  Experiment selection, the 'OK' button becomes active again (to prevent the user accidentally restarting a potentially long calculation).

If the user cancels the operation, the 'Running...' button changes to 'Cancelled' during the cleanup and then reverts back to 'Ok'.

#### Validation

The first step is that the validity of the Experiment Info files in the selected Experiments is verified.  An error is raised if the columns are different than expected, or when values in column are inconsistent with the expected datatype (for example when a numeric value is expected and a text string is present).  A more subtle error when there differences in attributes of replicates of the same condition is also detected.

#### TrackMate input parameters

The TrackMate calculations are influenced by a set of parameters, that do normally not require changing and which are specified in the **Paint Configuration** file.  If 'Verbose' is set, the parameters are displayed in the **Paint Console**. See below.

![paint-console-running](/Users/hans/JavaPaintProjects/doc/Pictures/paint-console-running.png) 

The processing time is constrained, by two parameters, specifying the maximum numbers of spots in an image and the maximum number of seconds per image. Experience has shown that when the number of spots in an image exceeds roughly 2,000,000, the tracking takes long and rarely delivers meaningful results. Setting a higher value for the **Threshold** (in the Experiment Info file) for that recording may be required. Also a limit on the number of processing seconds per image can be set. The two, in combination, prevent the pipeline choking up.  

In contrast to the general TrackMate parameters, the Threshold parameter needs to be set for every recording separately and therefore is specified in the Experiment Info file.



#### Recordings processed

The 'Experiment Info' files in the selected Experiments specify which Recordings are to be processed. An example of an 'Experiment Info' file is shown below. Recordings for which the 'Process Flag' is set to 'TRUE' will be processed. Allowable 'TRUE' values are TRUE, 1, Yes, Y and T; valid 'FALSE' values are FALSE, 0, No, N and F, all case insensitive. Other values will trigger an error.

For each Recording a 'Threshold' value is specified. A low value for Threshold will cause many features to be recognised as spots; high values will lead to fewer spots. The optimal value depends on the experiment and the user likely will need to resort to an iterative process to find the optimal values. To avoid long processing times,  initially high 'Threshold' values should be chosen      

![experiment-info](/Users/hans/JavaPaintProjects/doc/Pictures/experiment-info.png)

#### Paint Configuration file

The Paint Configuration file is a readable file in JSON format and contains 'Project' specific attributes. The file is read and written by software, but can also be inspected and edited with any normal plain text editor (Visual Studio Code, BBedit, Sublime). Below is the Paint Config file shown in Visual Code Studio.

In general users will not often have to edit the Paint Configuration and should only do so when they can assess the consequences of changes.  For information on the values in the TrackMate section, please refer to the [Track Mate manual](https://imagej.net/media/plugins/trackmate/trackmate-manual.pdf).

The file is 'self-healing', e.g., when necessary atrributes cannot be found, they are recreated with sensible defaults.  If the whole file is missing, for example for a fresh 'Project Root' it will be regenerated from scratch.

![paint-config-json](/Users/hans/JavaPaintProjects/doc/Pictures/paint-config-json.png)Only Project specific values are stored in the Paint Configuration file.  For system wide settings, the macOS Preferences mechanism is used to store system wide attributes in  ~/Library/preferences/Glyco-PAINT.plist (for Windows an equivalent implementation is provided). Users will generally not interact directly with this file,  but it is here where the current 'Project Root' and 'Images Root' are kept.  Below the Glyco-PAINT.plist is shown with Xcode's  plist viewer.

![preferences](/Users/hans/JavaPaintProjects/doc/Pictures/preferences.png)

#### TrackMate Results

Upon completion of a TrackMate run, two new files have been created in each 'Experiment' directory: 'Tracks' and 'Recordings'. The Tracks file contains the tracks for all the recordings in the Experiment, with for each caclulated attributes (the individual spots of the tracks are not saved).  The Recordings file is an evolution of Experiment Info file with spots, track and runtime information added:

- Number of Spots - the total number of spots in the Recording.
- Number of Tracks - the total number of tracks that were identified.
- Number of Spots in All Tracks - the sum of all the spots in those tracks (some spots are not assigned to tracks and this number is always lower than the Number of Spots). 
- Number of Frames - the number of frames in the recording (will normally be 2000).
- Run Time - the time TrackMate took to process the Recording.
- Time Stamp - for references purpose.

In addition to the creating of the 'Tracks' file and the updating of 'Recordings', two directories with images are created. The 'TrackMate Images' directory contains  the processed images in which the tracks are shown.  The images in the TrackMate and Brightfield directories are used in the Viewer application.

![trackmate-image](/Users/hans/JavaPaintProjects/doc/Pictures/trackmate-image.png) 

In the 'Brightfield Images' directory the corresponding Brightfield images are collected.

![brightfield-image](/Users/hans/JavaPaintProjects/doc/Pictures/brightfield-image.png) 



#### TrackMate Reproducibility 

Maybe surprisingly, TrackMate results are not entirely reproducible. For instance, two runs immediately after each other may yield slightly different results, such as the differing number of spots or tracks (one spot more or less, may cause one track more or less). Differences in the number of tracks can result in variations in certain square attributes. While these differences are negligible for practical purposes, they are worth noting.  



#### The Paint Console

Each application of the Glyco-PAINT pipeline opens its own Paint Cnsole. In the totel of the window is listed what application the Paint Console is attached to.

More information is logged in the Paint Console than can be seen at any one time and therefore scrolling is provided. Normally the end of the log is shown, so that new information is visible. The 'Scroll Lock' checkbox (left bottom) stops the auto scroll mechanism to allow the user to inspect earlier parts of the log undisturbed.

The 'Highlight Problems' button jumps to [ERROR] and [WARN ] lines in the log to alert the user on irregularities. Repeatedly clicking the button will jump to the next problem. 

A 'Save' button allows the saving of the information in the console window. Generally this is not needed as most information is saved automatically to a log file (see  next section).



#### Paint log files 

Every time the Glyco-PAINT Fiji plugin (or any other component of the pipeline) is started, a log file is opened in the 'Logs' directory under the 'Project Root' directory. Log files are named after the pipeline component that is logging. Files are sequentially number, so that older information is not overwritten. In a heavily analysed 'Project Root', the Logs directory may accumukate a large number of log files and cleaning up occasionaly may be warranted.   



#### Generate Squares

If the 'Generate Squares' checkbox was ticked before the the calculation was started, squares will be generated for all the Recordings in the selected Experiments. The user can chose several squares sizes, 20x20 has been found to be a good choices.

The following steps are followed:

1. Define the squares (with coordinates)
2. Determine in which squares the tracks of the recording are located.
3. Calculate the recording background density
4. Calculate for each square:
   - the Tau and R² (provided there are sufficient tracks,)
   - the density
   - the density ratio  
   - the varability
   - the Tau and R²

5. For each square determine whether it is 'selected', when all three conditions are met:
   - the variability < the maximum allowed variability 
   - the R² > the minimum required R²
   - the density ratio > the min required density ratio 

6. Calculate for the 'selected' squares in the recording:
   - the Tau and R²
   - the density
7. Calculate for all squares  ([Square calculations](https://github.com/jjabakker/JavaPaintProjects/blob/main/paint-generate-squares/src/main/java/generatesquares/calc/CalculateAttributes.java))
   - the Tau and R² (provided there are sufficient tracks,)
   - the density
   - the density ratio 
8. Calculate for all squares some statistical information on the tracks 
   - displacement
   - speed
   - duration


The results are stored in Experiment-level 'Squares' files



#### Calculation of Tau

Tau is a measure used to characterise the distribution of track durations. To calculate Tau, a frequency distribution is created from the track durations. These durations are then ordered and fitted with a one-phase exponential decay curve to obtain the Tau value. 

The Tau calculation is only performed if a minimum number of tracks is present (because with insuffcient tracks the calculation is unlikely to be meaningful).  The quality of curve fitting is expressed in an R² parameter. An R² value of 1 indicates a perfect fit, while values lower than 0.5 indicate a low-quality fit. The user-specified ‘Min allowable R-squared’ parameter sets a limit to the acceptable quality of fit.

To calculate a Tau value for the entire recording, all tracks within squares that meet the specified selection criteria are considered. These criteria include the minimum required density ratio, maximum allowable variability and neighbour state. 

Visual feedback on the fitting process is provided when the "Plot Curve Fitting" flag  (in the "Generate Squares" section) is set to true. In the 'Tau Fitting Plots' directory under the Experiment directories,  plots are gathered in 'Failed' and 'Success' sub directories. An example of such a plkot is shown below:

![tau-fit-plot](/Users/hans/JavaPaintProjects/doc/Pictures/tau-fit-plot.png)

The code can be found at: [Calculate Tau code](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateTau.java).



#### Calculation of Variability

The variability of a square calculation begins with overlaying a finer grid over the existing grid and determining the number of tracks in each grid element. The variability is then calculated as the quotient of the standard deviation and the mean of the grid track numbers. The figure below illustrates the variability for four fictional squares.

![variability](/Users/hans/JavaPaintProjects/doc/Pictures/variability.png)

The code can be found at: [Calculate Variability code](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateSquareAttributes.java).



#### Calculation of Diffusion Coefficient (Ext)

The 'Diffusion Coefficient' is calculated for each track in the recording that contains three or more spots, using the following formula. Here, **n** represents the dimensionality (in this case 2), and **t** is the time interval over which displacement is measured (0.05 s).

$$
MSD = \frac{1}{nr\ spots} \sum_{i=1}^{nr\ spots} \left( (x_i - x_0)^2 + (y_i - y_0)^2 \right)
$$

$$
\text{Diffusion Coefficient} = \frac{MSD}{2nt}
$$

The 'Diffusion Coefficient Ext' is a variation on the 'Diffusion Coefficient'. Here, the x and y coordinates of spot (i) are not compared to the first spot (0), but to the previous spot (i-1).

$$
MSD = \frac{1}{nr\ spots} \sum_{i = 1}^{nr\ spots} \left( (x_i - x_{i-1})^2 + (y_i - y_{i-1})^2 \right)
$$

$$
\text{Diffusion Coefficient Ext} = \frac{MSD}{2nt}
$$

The 'Median Diffusion Coefficient' and 'Median Diffusion Coefficient Ext' are square attributes and describes the median of the 'Diffusion Coefficient (Ext)' values of all tracks in the square.

The code can be found at: [Calculate Diffusion Coefficient code](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateSquareAttributes.java).



#### Calculation of background density

One of tbe criteria applied to for square selection is the 'Density  ratio'. Simply said, squares are only considered if they contyain 'significantly' more tracks then the 'majority' of the squares. A statistical procedure is applied in which squares are iteratively filtered with track counts exceeding a dynamically calculated threshold (mean + 2 * standard deviation). The mean is then recalculated and the calcuklation repeated until the mean stabilizes or a maximum number of iterations is reached.

![background](/Users/hans/JavaPaintProjects/doc/Pictures/background.png)




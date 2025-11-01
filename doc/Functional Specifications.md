



------

# Glyco-PAINT Pipeline 

------



[TOC]



# Introduction

This Java version of Glyco-PAINT will fully replace the current Python-based version. Functionally the versions are identical, but many improvements have been implemented. The most important improvements include:

- **Simplified user interface**. Fewer pipeline components, directly accessible as desktop applications (eliminating the need to use a development environment such as PyCharm as previously).
- **Improved validation**, making it less likely for incorrect input to interfere or invalidate with calculations.
- **Streamlined code**, recreated from the base, making it much easier to understand and maintain.
- **Better documentation**, making the code more  to users.
- **Improved version management**, fully utilising the GutHub release mechanism and installers for macOS and Windows. 

Whereas the functionality of this version is identical to the Python version, results from the Python version are not 100% reproducible. Partially this is because of some improvements in implementation of core algorithms, but more so because of inherent indeterministic behaviour of the TrackMate calculation engine itself (also with the Python environment itself, results are not 100% reproducible). Differences are small and do not impact the analysis and conclusion drawn from experiments.

The code for the Python version and the new Java version is available on: [Glyco-PAINT Python](https://github.com/Leiden-chemical-immunology/Glyco-PAINT) Github and [Glyco-PAINT Java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java). You no longer need access to the code for running the pipeline, but nevertheless familiarity with the core algorithms is desirable for more effective use of the pipeline  

In addition to the published code itself, a  Javadoc site provides a complete, web-based reference of all public classes and methods in the PAINT software. It is automatically generated from the code comments and published on GitHub Pages, allowing you to browse the documentation directly in your web browser. Each module and package includes detailed descriptions of its purpose, parameters, and return values, making it easier to understand and extend the software. You can access the latest version of the Javadoc at [Glyco-PAINT Javadoc](https://leiden-chemical-immunology.github.io/Glyco-PAINT-Java/).

Example images are published on Zenodo and can be used as  [Glyco-PAINT reference images](https://doi.org/10.5281/zenodo.17487086).

This document provides explanation on how to use the pipeline  (maintained as a Markdown document, on GitHub and distributed as a pdf file.   



# Setting Up

## System requirements

The Glyco-PAINT pipeline  developed, tested and heavily used on macOS, but should run on Windows and Linux also. For the latter two platforms support however is at a reduced level, because of the absence of a good testing environment. 

Glyco-PAINT is a computationally heavy application, so you will enjoy a powerful computer. All development and analysis for the Glyco-PAINT paper has been done on a MacBook Pro M3 Max with 36GB memory, but on a less powerful configuration it will also run, but slower.

You need to have **FiJi** installed. If you don't you can download it from the [FiJi downloads](https://imagej.net/software/fiji/downloads) site.  **TrackMate** normally comes automatically with a FiJi installation. Information on [TrackMate](https://imagej.net/plugins/trackmate/) is available and you can also download it from there if needed.

The pipeline depends on the presence of the **Java 8 JRE** (Java Runtime Environment). Chances are that it is available on your computer already. You can check on macOS from a terminal with:

```
/usr/libexec/java_home -V
```

On windows use Explorer to check the directories:

```
C:\Program Files\Java\
C:\Program Files (x86) Java\
```

If the Java 8 JRE is not present, you can download it from [Adoptium Java 8 JRE](https://adoptium.net/en-GB/temurin/releases?version=8&os=any&arch=any) (make sure you select JRE and not JDK).

  

## Pipeline components

The pipeline consist of a Fiji plugin that performs the TrackMate calculations and a number of separate apps. The two must important apps are:

- **Generate Squares**, which allows you to generate squares over the processed TrackMate images and calculates the square attributes such as Tau, Density, etc.
- **Viewer**, which allows you to visually inspect the TrackMate results in comparison with the brightfield image. You can change square selection parameters to determine which squares are included in subsequent analysis.

The latest version of the plugin integrates the Generate Squares functionality, so the Generate Squares may be retired over time.

In addition to these main apps there are two simple utilities:

- **Get Omero** which allows you to organise downloaded Omero files without extensive manual intervention. This addresses a peculiarity in Omero and is not part of the Glyco-PAINT pipeline.

- **Create Experiments** which helps you create an Experiment Info file that contains the instructions necessary to run the pipeline. The app does not do anything that a simple text based editor cannot also do, but you may find it convenient.         

  

## Installing the pipeline

There are multiple ways to install the Glyco-PAINT pipeline. The easiest is through a self extracting installer, likely called something like: Glyco-PAINT-Installer.sh.

For an easy installation route, agree with the developers at Leiden LIC access to the OneDrive Glyco-PAINT distribution site.  Download the installer file (typically with a 500 MB size) to any folder on your computer (typically your ~/Downloads folder). Open a terminal and execute the following commands

```
cd ~/Downloads
./Glyco-PAINT-installer.sh
```

The installer will create a directory Glyco-PAINT in the ~/Applications directory and copy the four apps (Generate Squares, Viewer, Get Omero and Create Experiment) to it and will subsequently attempt to copy the plugin jar to the Fiji plugins directory. 

The jar name will be similar to  **paint-fiji-plugin-0.0.3-SNAPSHOT-jar-with-dependencies.jar**. The number sequence 0.0.3 will be incremented with subsequent releases. The **SNAPSHOT** may be present,  indicating  this is a development version, or not, indicating a formal release version.

The Glyco-Paint apps can be started by simply clicking on them in the ~/Applications/Glyco-PAINT folder. There is no good reason for it, but you can start the Glyco-PAINT apps also from the command line. For that, you need to open the App bundles and locate the jar files in the Contents/Java directory. As example for Get Omero:

```
cd ~/Applications/Glyco-PAINT/Get Omero/Contents/Java
java -jar paint-get-omero-0.0.3-SNAPSHOT-jar-with-dependencies.jar
```



# Project and Image Root

Central in the pipeline is the **Project Root**, a directory containing subdirectories that represent **Experiments**. Experiment directories contain as a minimum an **Experiment Info** file that contains information about **Recordings** of the **Experiment**.

Parallel to the Project Root, the pipeline depends on an **Images Root** directory, where the microscopy data is stored. The Experiment structure in the Project Root needs to be exactly replicated in the Images Root, so for example, if under the Project Root, Experiments 'Exp A', 'Exp B' and 'Exp C' exist, those same directories are expected to be present under the Images Root. The Experiment directories in the Images Root contain the files recorded with the microscope. For every recording two files are needed: the multi-frame recording, with typically 2000 frames, and a single-frame brightfield image.

The images are likely downloaded from Omero (for which the pipeline provides a **Get Omero** app) or may be copied in any other way. The names of the recordings can be chosen freely, with the condition that the name of the brightfield image is equal to the multi-frame recording with '-BF' attached.

In the Project Root directory, a **Paint Configuration** file exists, in which important parameters for the pipeline are stored. If the file does not exist, for example in a new installation, it will be created automatically with default parameters. The user should confirm that the defaullt values are appropriate or make changes.



# The TrackMate plugin

The Glyco-PAINT plugin is started, after starting Fiji, by selecting from the Plugins menu, the Glyco-PAINT and Run options. A dialog box like the one shown below pops up.

![trackmate-plugin](./Pictures/trackmate-plugin.png)

A Paint Console is also opened to provide you with feedback on the status and progress of operations, 

![paint-console-initial](./Pictures/paint-console-initial.png)  

## The plugin user interface

On top, the currently selected **Project Root** and **Images Root** are shown; the Browse buttons on the right allow users to change that selection.

Immediately below, you can select to have squares generated after TrackMate processing is completed. If the checkbox is ticked, the parameters can be entered.

Below that, a list of **Experiments** is displayed. TrackMate (and Generate Squares)  calculations are performed on selected Experiments only. The **Select All** and **Clear All** buttons facilitate easy selection of Experiments.

At the bottom of the dialog, three checkboxes are shown.

- If the **Save Experiments** option is clicked, the current selection of Experiments will be saved.
- The **Verbose** option, when clicked, provides more detailed feedback to the user.
- The **Sweep** option, used only in special analysis cases, performs a parameter sweep. In this process, TrackMate calculations are performed with varying parameters. For more information on this option, please refer to ….

Pressing the Ok button starts the calculation. Pressing the Cancel button when a calculation is running stops it. When no calculations are running, pressing Cancel wil close the dialog.



## Experiments

The experiments that are shown, are subdirectories directly under the selected Project Root. To be shown as Experiment the directory needs to contain an **Experiment Info** file.  If the user selects a different Project Root, the display of Experiments is refreshed.

Note: If 'Sweep' calculations are performed, a Sweep directory will be created in the Project Root which will contain an 'Experiment Info' file,  but it will never be shown as Experiment.

A previously saved selection of 'Experiments' is stored in the **Paint Configuration** file that is present in every Project Root. This file will be automatically created if it does not exist. If for some reason it is corrupted, you can safely delete it.



## Run TrackMate



#### The overall flow

When the Project and Image Roots have been set and Experiments selected, pressing the 'Ok' button starts the operation. The 'Ok' button changes to 'Running...' for as long as the operation runs. Only the 'Cancel' button is enabled during this period and can be used to interrupt the calculation.

If the operations completes normally, the 'Running...' button reverts back to 'OK', but remains disabled. Only after changing the  Experiment selection, the 'OK' button becomes active again (to prevent the user accidentally restarting a potentially long calculation).

If the user cancels the operation, the 'Running...' button changes to 'Cancelled' during the cleanup and then reverts back to 'Ok'.



#### Validation

The first step is that the validity of the Experiment Info files in the selected Experiments is verified.  An error is raised if the columns are different than expected, or when values in column are inconsistent with the expected datatype (for example when a numeric value is expected and a text string is present).  A more subtle error when there differences in attributes of replicates of the same condition is also detected.



#### TrackMate input parameters

The TrackMate calculations are influenced by a set of parameters, that do normally not require changing and which are specified in the **Paint Configuration** file.  If 'Verbose' is set, the parameters are displayed in the **Paint Console**. See below.

![paint-console-running](./Pictures/paint-console-running.png) 

Information on TrackMate and TrackMate parameters is found in the [TrackMate manual](https://imagej.net/media/plugins/trackmate/trackmate-manual.pdf).

The processing time is constrained, by two parameters:

- The maximum numbers of spots in an image. Experience has shown that when the number of spots in an image exceeds roughly 2,000,000, the tracking takes long and rarely delivers meaningful results. Setting a higher value for the **Threshold** (in the Experiment Info file) for that recording may be required
- The maximum number of seconds per image provides a hard cut-off on calculation time.

The two, in combination, prevent the pipeline choking up.  In contrast to the general TrackMate parameters, the Threshold parameter is set for every recording separately in the Experiment Info file.



#### Recordings processed

The Experiment Info files in the selected Experiments specify which Recordings are to be processed. An example of an Experiment Info file is shown below. Recordings for which the **Process Flag** is set to 'TRUE' will be processed. Allowable 'TRUE' values are TRUE, 1, Yes, Y and T; valid 'FALSE' values are FALSE, 0, No, N and F, all case insensitive. Other values will trigger an error.

For each Recording a **Threshold** value is specified. A low value for Threshold will cause many features to be recognised as spots; high values will lead to fewer spots. The optimal value depends on the experiment and the user likely will need to resort to an iterative process to find the optimal values. To avoid long processing times,  initially high 'Threshold' values should be chosen      

![experiment-info](./Pictures/experiment-info.png)

#### Paint Configuration file

The P**aint Configuration** file is a readable file in JSON format and contains Project specific attributes. The file is read and written by software, but can also be inspected and edited with any normal plain text editor (Visual Studio Code, BBedit, Sublime). Below is the Paint Config file shown in Visual Code Studio.

In general users will not often have to edit the Paint Configuration and should only do so when they can assess the consequences of changes.  For information on the values in the TrackMate section, please refer to the [Track Mate manual](https://imagej.net/media/plugins/trackmate/trackmate-manual.pdf).

The file is 'self-healing', e.g., when necessary  cannot be found, they are recreated with sensible defaults.  If the whole file is missing, for example for a freshly created Project Root, it will be regenerated from scratch.

![paint-config-json](./Pictures/paint-config-json.png)Only Project specific values are stored in the Paint Configuration file.  For system wide settings, the macOS Preferences mechanism is used to store attributes in  ~/Library/preferences/Glyco-PAINT.plist (for Windows an equivalent implementation is provided). You will generally not interact directly with this file,  but it is here where the current 'Project Root' and 'Images Root' are kept.  Below the Glyco-PAINT.plist is shown with Xcode's  plist viewer. Also the **Log Level** can be adjusted here. The standard Log Level is **Info**, but for more detailed information **Debug** can be chosen. Less information can be displayed by choosing **Error** or **Warn**.  

![preferences](./Pictures/preferences.png)

#### TrackMate Results

Upon completion of a TrackMate run, two new files have been created in each Experiment directory: **Tracks** and **Recordings**. The Tracks file contains the tracks for all the recordings in the Experiment, with for each calculated attributes (the individual spots of the tracks are not saved). The Recordings file is an evolution of Experiment Info file with spots, track and runtime information added:

- **Number of Spots** - the total number of spots in the Recording.
- **Number of Tracks** - the total number of tracks that were identified.
- **Number of Spots in All Tracks** - the sum of all the spots in those tracks (some spots are not assigned to tracks and this number is always lower than the Number of Spots). 
- **Number of Frames** - the number of frames in the recording (will normally be 2000).
- **Run Time** - the time TrackMate took to process the Recording.
- **Time Stamp** - for references purpose.

In addition to the creating of the 'Tracks' file and the updating of 'Recordings', two directories with images are created. The **TrackMate Images** directory contains  the processed images in which the tracks are shown.  The images in the TrackMate and Brightfield directories are used in the Viewer application.

<div align="center">
<img src="./Pictures/trackmate-image.png" alt="trackmate-image" style="zoom:33%;" /> 
</div>


In the **Brightfield Images** directory the brightfield images for  recording are collected.

<div align="center">
<img src="./Pictures/brightfield-image.png" alt="brightfield-image" style="zoom:33%;" /> 
</div>



#### TrackMate Reproducibility 



#### The Paint Console

Each application of the Glyco-PAINT pipeline opens its own Paint **Console**. In the title of the window the name of  application the Paint Console is attached to, is listed.

More information is logged in the Paint Console than can be seen at any one time and therefore scrolling is provided. Normally the end of the log is shown, so that new information is visible. The **Scroll Lock** checkbox (left bottom) stops the auto scroll mechanism to allow you to inspect earlier parts of the log undisturbed.

The **Highlight Problems** button jumps to [ERROR] and [WARN ] lines in the log to alert you to irregularities. Repeatedly clicking the button will jump to the next problem. 

A **Save** button saves the information in the console window to a file. Generally this is not needed as most information is already saved automatically to a log file (see  next section).



#### Paint log files 

Every time the Glyco-PAINT Fiji plugin (or any other component of the pipeline) is started, a log file is opened in the **Logs** directory under the Project Root directory. Log files are named after the pipeline component that is logging. Files are sequentially numbered, so that older information is not overwritten. In a heavily analysed 'Project Root', the Logs directory may accumulate a large number of log files and cleaning up  may be warranted.   



#### Generate Squares

If the 'Generate Squares' checkbox was ticked before the calculation was started, squares will be generated for all the Recordings in the selected Experiments. You can choose several squares sizes; 20x20 has been found to be a good choice.

The following steps are followed:

1. Define the squares (with coordinates)
2. Determine in which squares the tracks of the recording are located.
3. Calculate the recording background density
4. Calculate for each square ([CalculateSquareAttributes.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateSquareAttributes.java):  
   - the Tau and R² (provided there are sufficient tracks)  
   - the density
   - the density ratio  
   - the variability
5. Calculate for all squares some statistical information on the tracks 
   - displacement
   - speed
   - duration
6. For each square determine whether it is 'selected', i.e., when all three conditions are met:
   - the variability < the maximum allowed variability 
   - the R² > the minimum required R²
   - the density ratio > the min required density ratio 
7. Calculate for the combined selected' squares in the recording:
   - the Tau and R²
   - the density


The results are stored in Experiment-level **Squares** files



#### Calculation of Tau

Tau is a measure used to characterise the distribution of track durations. To calculate Tau, a frequency distribution is created from the track durations. These durations are then ordered and fitted with a one-phase exponential decay curve to obtain the Tau value ([CalculateTau.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-shared-utils/src/main/java/paint/shared/utils/CalculateTau.java)).

The Tau calculation is only performed if a sufficient number of tracks is present (because with too few tracks the calculation is unlikely to be meaningful).  The quality of curve fitting is expressed in an R² parameter. An R² value of 1 indicates a perfect fit, while values lower than 0.5 indicate a low-quality fit. The user-specified ‘Min allowable R-squared’ parameter sets a limit to the acceptable quality of fit.

To calculate a Tau value for the entire recording, all tracks within squares that meet the specified selection criteria are considered. These criteria include the minimum required density ratio, maximum allowable variability and neighbour state. 

Visual feedback on the fitting process is provided when the "Tau Fitting Plots" flag  (in the "Generate Squares" section) is set to true. In the 'Tau Fitting Plots' directory under the Experiment directories,  plots are gathered in 'Failed' and 'Success' sub directories. An example of such a plot is shown below:

<img src="./Pictures/tau-fit-plot.png" alt="tau-fit-plot" style="zoom:33%;" />



#### Calculation of Variability

The variability of a square calculation begins with overlaying a finer grid over the existing grid and determining the number of tracks in each grid element. The variability is then calculated as the quotient of the standard deviation and the mean of the grid track numbers. The figure below illustrates the variability for four fictional squares.

![variability](./Pictures/variability.png)

The code can be found at: [CalculateSquareAttributes.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateSquareAttributes.java).



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

The code can be found at: [TrackAttributeCalculations.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-fiji-plugin/src/main/java/paint/fiji/tracks/TrackAttributeCalculations.java)



#### Calculation of background density

One of the criteria applied to for square selection is the 'Density  ratio'. Simply said, squares are only considered if they contain 'significantly' more tracks than the 'majority' of the squares. A statistical procedure is applied in which squares are iteratively filtered with track counts exceeding a dynamically calculated threshold (mean + 2 * standard deviation). The mean is then recalculated and the calculation repeated until the mean stabilises or a maximum number of iterations is reached.

The code can be found at: [SquareUtils.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/SquareUtils.java)

<img src="./Pictures/background.png" alt="background" style="zoom:33%;" />



## TrackMate Sweep mode 



# Viewer

With the Viewer you can inspect the what tracks have been generates for recodings and what squares meet the selection criteria.

The first dialog looks similar to the TrackMate plugin and allows the selection of the **Project Root** and **Experiments** you want to  view.  You can also specify the **Images Root**, but this is only necessary if you want to replay the recordings as made by the microscope. Notice that seelections you have made previously, fopr excample in the TrackMate plugun or Generate Squares have been preserved. 

<div align="center">
  <img src="./Pictures/viewer-1.png" alt="viewer-1" style="zoom:33%;" />
</div >
Having made all selections, the actual Viewer dialog is displayed. Three panels are visible: 

- On the left an attribute panel that display information on the current recording. 
- In the middle a panel with the TrackMate image on the left and the brightfield image on the right. 
- On the right a panel with some options to control the display and to provide extra functionality.

In the TrackMate image, those squares are displayed that meet the slection criteria specified during the generation of the squares. Thos criteria are viaible in the attribute panel on the left (Min Density Ratio, Max Variability, Min R²  and Neighbour Mode). 

![viewer-2](./Pictures/viewer-2.png) 

The checkboxes in the control panel on the right determine whether borders are shown, whether squares are shaded and whether label or square numbers is displayed or not.

## Select Squares

Pressing the **Select Squares** button in the control panel, causes a Square Control dialog to become visible. Moving the sliders will change the criteria for square selection and you will see the squares selection change dynamically. You can assign the current settings to just the Recording, all Recordings in the same Experiment or to all Recordings currently loaded in the Viewer. In that case those settings will be presered for the current Viewer settings and stored in a **Recording Override** file in the **Viewer** directory in the Project Root. 

<img src="./Pictures/select-squares-dialog.png" alt="select-squares-dialog" style="zoom:33%;" />

## Assign Cells

Pressing the **Assign Cells** button, causes the Assign Cells dialog to be shown. You can select squares, by pressing the left mouse button and dragging the mouse. Selected squares are assigned to cell 1 till 6 (or unassigned) when you click the **Assign** button.  The cell assignments are preserved for the current Viewer settings and stored in a **Square Override** file in the **Viewer** directory in the Project Root. 

<div align="center">
<img src="./Pictures/assign-cells-dialog.png" alt="assign-cells-dialog" style="zoom:33%;" />
</div>
## Filter Recordings



![filter-recordings-dialog](./Pictures/filter-recordings-dialog.png)


# Get Omero



# Create Experiment



# Analysis in R



# Development

## Github

The code base is published on Github, which allows the code to be checked and peer reviewed. An overview of the main windo is shown below. The pipeline is maintained as a project with multiple modules, some of which produce the plugin and the apps. 

- paint-create-experiment contains the Create Experiment code
- paint-fiji-plugin contains the code for FiJi plugin
- paint- generate-squares contains the Generate Squares code
- paint-get-omero contains the Get Omero code
- paint-viewer contains the Viewer code

Others are for supporting and development purposes:

- paint-shared-utils contains code shared by the applications above
- shell-scripts contains scripts to facilitate maintainenance tasks 
- reference-case  contains the csv files associated with the reference images



![github](./Pictures/github.png)



## IDEA

The development environment used is Jetbrain's IntelliJ IDEA. It can be downloaded from the [IDEA](https://www.jetbrains.com/idea/) website. For academic use, free licences to the professional edition can be obtained from the [Student Pack](https://www.jetbrains.com/academy/student-pack/) site. It is not necessary to download IDEA for just running the pipeline, but it is recommended for reviewing the code.

In the picture below the IDEA environment for the Glyco-PAINT pipeline is shown.

![idea](./Pictures/idea.png)

If you want to reciew code, after having installed IDEA, you can **clone** a project from Github, using [File] - [New] - [Project from Version Control]. You specify as URL https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java and you select a folder on your harddrive where you want the code to be installed.

With a clone, you get a copy of the published Glyco-PAINT source code. With [Git] - [Pull] you can refresh the locally available code with the latest GitHub information.

In the local environment you can experiment, browse code, debug code, change code as you please. Uploading changes is possible, but requires special authorisation. 


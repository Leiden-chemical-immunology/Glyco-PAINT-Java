<p style="text-align:center">
  <img src="./Pictures/leiden.png" alt="Glyco-PAINT logo" style="max-width:180px; height:auto; margin-bottom:20px;" />
</p>



<h1 style="text-align:center; margin:0; font-size:2.2em;">Glyco‑PAINT Application Processing Pipeline</h1>
<h3 style="text-align:center; margin:6px 0 18px; font-weight:400;">Java implementation with FiJi/TrackMate integration</h3>

<hr style="width:120px; border:0; border-top:2px solid #555; margin:18px auto 24px;" />

<p style="text-align:center; margin:0; font-size:1.1em;">Version 0.0.132 · Updated: 5 December 2025</p>

<p style="text-align:center; margin:0;"><strong>Authors:</strong> J. Bakker</p>
<p style="text-align:center; margin:4px 0 18px; color:#555;">Leiden Chemical Immunology | LIC, Leiden University</p>

<p style="text-align:center" style="margin:0;">
  <strong>Code:</strong>
  <a href="https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java">Glyco‑PAINT Java</a> ·
</p>
<p style="text-align:center" style="margin:6px 0 18px;">
  <strong>Docs:</strong>
  <a href="https://leiden-chemical-immunology.github.io/Glyco-PAINT-Java/">JavaDoc site</a> ·
  <strong>Reference images:</strong> <a href="https://doi.org/10.5281/zenodo.17487086">Zenodo</a>
</p>


<hr style="width:120px; border:0; border-top:1px solid #aaa; margin:18px auto 26px;" />

<p style="text-align:center" style="max-width:700px; margin:0 auto; color:#333;">
  This manual documents the Glyco‑PAINT pipeline: installation, configuration, TrackMate processing, square generation, viewing, and analysis. It is maintained in Markdown and exported to PDF for distribution.
</p>





























[TOC]





# Introduction


This Java version of Glyco-PAINT fully replaces the Python-based version. While functionality remains identical, several improvements have been introduced:

- **Simplified user interface**: fewer components, accessible directly as desktop apps (no need for PyCharm).
- **Improved validation**: reduces the chance of incorrect input interfering with calculations.
- **Streamlined code**: rebuilt from scratch, easier to understand and maintain.
- **Better documentation**: more accessible for users.
- **Improved version management**: GitHub releases and installers for macOS and Windows.

Although functionality matches the Python version, results cannot be reproduced 100% due to algorithmic refinements and the non-deterministic nature of TrackMate. These differences are minor and do not affect conclusions.

The code is available on GitHub:  
- [Python version](https://github.com/Leiden-chemical-immunology/Glyco-PAINT)  
- [Java version](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java)  

A full [Javadoc reference](https://leiden-chemical-immunology.github.io/Glyco-PAINT-Java/) is also published online.  

Example images are available on Zenodo as [Glyco-PAINT reference images](https://doi.org/10.5281/zenodo.17487086).

This document explains how to use the pipeline. It is maintained as Markdown on GitHub and distributed as a PDF.


<div style="page-break-inside: avoid;">

# Setting Up

## System Requirements

The Glyco-PAINT pipeline was developed and tested on macOS, but also runs on Windows and Linux (with reduced support). Because it is computationally heavy, a powerful computer is recommended. For example, the Glyco-PAINT paper was analyzed on a MacBook Pro M3 Max with 36 GB memory.

Fiji and Java 8 JRE needs to be installed:

- **FiJi**: Download from the [FiJi site](https://imagej.net/software/fiji/downloads).  
  
  - Windows: preferably install in `ProgramFiles`, `ProgramFiles(x86)`, or `AppData\Local`.  
  - macOS: install in `~/Applications` or `/Applications`.  

- **Java 8 JRE**: Check if installed. :  
  
  On macOS:  
  ```bash
  /usr/libexec/java_home -V
  ```
  
  On Windows:  
  ```powershell
  where java
  ```
  If you see `jdk-8` or `1.8.0`, you’re set. Otherwise, download from [Adoptium Java 8 JRE](https://adoptium.net/en-GB/temurin/releases?version=8&os=any&arch=any) (select **JRE**, not JDK).

</div>





## Pipeline Components

The pipeline consists of a FiJi plugin (for TrackMate calculations) and several apps:

| Component             | Purpose                                           | Essential? |
|-----------------------|---------------------------------------------------| ---------- |
| **Viewer**            | Inspect TrackMate results with brightfield images | Yes        |
| **Generate Squares**  | Calculate square attributes (Tau, Density, etc.)  | Yes        |
| **Plugin**            | Performs the TrackMate calculations               | Yes        |
| **Get Omero**         | Organize downloaded Omero files                   | Optional   |
| **Create Experiment** | Build Experiment Info file (convenience utility)  | Optional   |







## Installing the Pipeline

The easiest way to install Glyco-PAINT is via the official installer. It lets you select which components to install.

1. **Access the Installer**  
   Request access to the Glyco-PAINT distribution site on OneDrive (Leiden LIC).

2. **Download**  
   Choose the installer for macOS or Windows (~400 MB). Save it to any folder (e.g. `Downloads`).

3. **Run Installer**  
   Double-click the file. A `Glyco-PAINT` directory will be created, containing the selected apps.

4. **FiJi Plugin Setup**  
   The installer attempts to locate FiJi automatically:  
   - **Windows**: checks `%ProgramFiles%`, `%ProgramFiles(x86)%`, `%LOCALAPPDATA%` for `Fiji`, `Fiji-win64`, `Fiji-win32`, or `Fiji-Windows`.  
   - **macOS**: checks `~/Applications/Fiji.app` and `/Applications/Fiji.app`.  

   If FiJi is found, the plugin is copied into `Fiji/plugins` (replacing old versions).  
   If not found, you’ll be prompted to specify the path. As a fallback, the plugin is placed in `Glyco-PAINT/plugins`, and you must copy it manually.



<img src="Pictures/Installer.png" alt="Installer" style="zoom:33%;" />



# System Features

## Project and Image Root

At the core of the Glyco-PAINT pipeline are two directories:

- **Project Root**: contains subdirectories for each experiment. 
- **Images Root**: stores the microscopy data, mirroring the structure of the Project Root.

If your Project Root contains 'Exp A', 'Exp B' and 'Exp C', then your Images Root must contain the same directories:

Each **Experiment directory** in the Images Root should contain for every Recording:
- A multi-frame recording (typically ~2000 frames).

- A single-frame brightfield image. 
  
  > [!NOTE] 
  > **Naming Convention**: The brightfield image must match the recording name with `-BF` appended.

Images are usually downloaded from **Omero** (using the *Get Omero* app).   Alternatively, they can be copied manually. 





## Paint Configuration File

Each Project Root contains a **Paint Configuration file** (`Paint Configuration.json`). This file stores project-specific parameters in JSON format.

- If missing, it is automatically created with default values.  
- If attributes are missing, they are regenerated with sensible defaults (“self-healing”).  
- If corrupted, you can safely delete it — the pipeline will recreate it.

You can edit the file with any text editor (e.g., Visual Studio Code, BBEdit, Sublime). However, changes should only be made if you understand their impact. Editing with Visual Code Studio has the advantage that syntactic correctness is checked.

![paint-config-json](./Pictures/paint-config-json.png)


## System-Wide Settings

System-wide preferences are stored separately:
- **macOS**: `~/Library/preferences/Glyco-PAINT.plist`
- **Windows**: `C:\Users\<username>\Library\Preferences\Glyco-PAINT.plist`

These files store attributes such as:
- Current Project Root and Images Root
- Log level (Info, Debug, Warn, Error)

You generally won’t edit these directly, but they control global pipeline behavior.






## Paint Console

Each Glyco-PAINT application opens its own **Paint Console** window. The console provides real-time feedback on the status and progress of operations.

- **Title bar**: shows the name of the application (e.g., *TrackMate*).
- **Scrolling**: the console auto-scrolls to the latest log entries.  
  - Enable *Scroll Lock* to pause auto-scrolling and inspect earlier logs.
- **Highlight Problems**: jumps directly to `[ERROR]` or `[WARN]` lines.  
  - Clicking repeatedly cycles through all issues.
- **Save**: exports the console contents to a file.  
  - Usually unnecessary, since logs are already saved automatically.
- **Resizable window**: adjust the size to fit your workflow.

![paint-console-initial](Pictures/paint-console-initial.png)



## Paint Log Files

Every time a Glyco-PAINT component runs, a log file is created in the **Logs** directory under the Project Root.

- **Naming**: files are named after the component (e.g., `TrackMateOnProject.log`).  
- **Numbering**: sequentially numbered to avoid overwriting older logs.  
- **Accumulation**: heavy analysis may generate many logs; occasional cleanup is recommended.





# Main process flow

To run a complete analysis, follow these steps in order:

1. Prepare the image data  

   Make sure that all recording images—both the film and the brightfield—are placed inside an Experiment directory located under the Images Root.

2. Create the Experiment Info file  

   In the Project Root, create a corresponding Experiment directory (with the same name as in the Images Root).  

   Inside it, add the Experiment Info file.  

   You can generate this file using the Create Experiment app (see [Create Experiment](#Create-Experiment)), or you can build it manually.

3. Run TrackMate  

   Execute the TrackMate plugin to perform the tracking step.

4. Generate Squares  

   Produce the square regions—either directly from the TrackMate plugin or via the stand‑alone Squares Generator app.

5. Inspect the results  

   Open the viewer to explore and validate the processed data.





# TrackMate Plugin

The Glyco-PAINT plugin is launched from FiJi by selecting **Plugins → Glyco-PAINT → Run**. A dialog window appears with options for configuring and running TrackMate.

<img src="Pictures/trackmate-plugin.png" alt="trackmate-plugin" style="zoom:80%;" />

- **Project Root / Images Root**: shown at the top, with *Browse* buttons to change directories.
- **Generate Squares**: optional checkbox to run square generation immediately after TrackMate processing. Parameters can be entered if enabled.
- **Experiment Selection**: lists all experiments found in the Project Root.  
  - Only experiments containing an *Experiment Info* file are shown.  
  - Use *Select All* / *Clear All* for quick selection.  
  - Saved selections are stored in the Paint Configuration file.
- **Options**:
  - *Save Experiments*: saves the current experiment selection.
  - *Verbose*: provides detailed feedback in the Paint Console.
  - *Sweep*: enables parameter sweep mode (see below).

At the bottom of the dialog:
- **OK**: starts processing. Changes to *Running…* while active.  
- **Cancel**: stops a running calculation or closes the dialog when idle.





## Running TrackMate

1. **Validation** 
   Experiment Info files are checked for:
   - Correct column names
   - Numeric values in numeric columns
   - Valid boolean values (TRUE, 1, Yes, Y and T or FALSE, 0, No, N and F (all case-insensitive))
   - Consistency across replicates
   In addition, the format of the Paint Configuration is checked.

2. **TrackMate Parameter Setup**
   TrackMate parameters are read from the Paint Configuration file. If *Verbose* is enabled, they are displayed in the Paint Console.

3. **Generate Squares Setup**

   If the Run Generate Squares after TrackMate box is checked, parameters can be set via the user interface.

4. **Experiments and Recordings selection** 
   
   Experiments are selected by checkboxes. Which recordings are processed is determined on the context of the Experiment Info files in the Experiment directories. Recordings for which the Process Flag is to TRUE are processed, others are ignored
   
   ![experiment-info](./Pictures/experiment-info.png) 
   
5. **Processing** 
   Selected experiments are processed sequentially. Each recording generates:

   - **Tracks file**: contains calculated track attributes.
   - **Recordings file**: updated Experiment Info with spots, tracks, and runtime details.
   - **Images**: processed TrackMate images and brightfield images for use in the Viewer.


<table style="border:none;">
  <tr>
    <td style="border:none; padding-right:10px;">
      <img src="./Pictures/trackmate-image.png" alt="trackmate-image" style="width:95%; height:auto;" />
    </td>
    <td style="border:none;">
      <img src="./Pictures/brightfield-image.png" alt="brightfield-image" style="width:95%; height:auto;" />
    </td>
  </tr>
</table>



**Performance Limits** 
Two parameters prevent excessive runtime:

- **Max spots per image** (default: 2,000,000). 
- **Max seconds per image** (default: 2000). 

These values can be adjusted in the Paint Configuration file (in the TrackMate section).





## TrackMate Sweep Mode

**Sweep Mode** allows systematic variation of TrackMate parameters to assess their impact on results.

### How to use
1. Enable the **Sweep** checkbox in the plugin dialog.  
2. Define variations in `Paint Sweep Configuration.json` located in the **Project Root**.  
3. Specify which parameters to sweep.

### Sweep Settings
- The **Sweep** value in *Sweep Settings* must be set to `true`.  
- In the *TrackMate Sweep* section, parameters to be swept are marked with a boolean value.  
- For each selected parameter, a corresponding section must exist where values are defined.  
  - Example: if `MAX_FRAME_GAP` is selected, a `MAX_FRAME_GAP` section must be present with values (e.g. `3` and `4`).  

> [!NOTE]  
> Label names are arbitrary, as long as they are distinct. For example, values called *Value 3* and *Value 4* could just as well be named *A* and *B*.

### Sweep Directory
- A **Sweep** directory is created in the **Process Root**.  
- Within it, subdirectories represent scenarios (e.g. `[Threshold-30]`, `[Threshold-20]`).  
- Each scenario is structured like a Project Root, containing:
  - Experiment directories  
  - A Paint Configuration file  
- TrackMate runs for each scenario, generating **Tracks**, **Recordings**, and **Squares** files.

### Results Compilation
- After all scenarios are processed, results are compiled in the **Sweep** directory.  
- Each data file includes a **Case** field, allowing results to be distinguished during downstream analysis (e.g. in R).




# Generate Squares

The **Generate Squares** functionality can be run either:
- As part of the TrackMate processing (by enabling the checkbox in the plugin dialog), or
- As a standalone app.

 If the *Save Experiments* option was previously enabled, these selections will be preloaded.




## Calculation Steps

Squares are generated for all recordings in the selected experiments. The workflow is:

1. **Define squares** (with coordinates).
2. **Assign tracks**: determine which tracks fall into each square.
3. **Calculate background density** for the recording.
4. **Calculate attributes for each square**:
   - Tau and R² (if sufficient tracks are present).
   - Density.
   - Density ratio.
   - Variability.
5. **Calculate statistical information** across all squares:
   - Displacement.
   - Speed.
   - Duration.
6. **Determine visibility**: a square is considered *visible* if all conditions are met:
   - Variability < maximum allowed.
   - R² > minimum required.
   - Density ratio > minimum required.


## Parameters

Several parameters control square generation. These can be set via the plugin dialog or in the Paint Configuration file:

| Parameter                       | Description                                 | Default |
|---------------------------------| ------------------------------------------- | ------- |
| **Number of Squares**           | Grid size (e.g., 20×20 is recommended)      | 400     |
| **Min Required R²**             | Minimum curve fit quality for visibility    | 0.1     |
| **Min Required Density Ratio**  | Minimum density ratio for visibility        | 2.0     |
| **Max Allowable Variability**   | Upper limit for variability                 | 10.0    |
| **Min Tracks to Calculate Tau** | Minimum tracks required for Tau calculation | 20      |



Note:

- **Square size**: 20×20 has been found to balance resolution and performance.  
- **Filtering**: visibility criteria ensure only meaningful squares are included in downstream analysis.  
- **Integration**: when run via the plugin, Generate Squares is executed automatically after TrackMate if the option is enabled.





## Calculation of Tau

Tau is a measure used to characterise the distribution of track durations.   To calculate Tau:

1. A frequency distribution is created from the track durations.  
2. These durations are ordered and fitted with a one-phase exponential decay curve. 
3.  Tau value is obtained ([CalculateTau.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-shared-utils/src/main/java/paint/shared/utils/CalculateTau.java)).

### Requirements
- Tau calculation is only performed if a sufficient number of tracks is present (to ensure meaningful results).  
- The quality of curve fitting is expressed in an **R² parameter**:  
  - R² = 1 → perfect fit  
  - R² < 0.5 → low-quality fit  
- The user-specified *Min allowable R-squared* parameter sets the threshold for acceptable fit quality.

### Whole Recording Tau
To calculate Tau for an entire recording, all tracks within squares that meet the selection criteria are considered. These criteria include:
- Minimum required density ratio  
- Maximum allowable variability 
- Minimum required  R²
- Neighbour state  

### Visual Feedback
If the *Tau Fitting Plots* flag (in the *Generate Squares* section) is set to `true`, plots are generated and stored in the *Tau Fitting Plots* directory under the Output directory of each experiment. Plots are grouped into *Failed* and *Success* subdirectories.

<img src="./Pictures/tau-fit-plot.png" alt="tau-fit-plot" style="zoom:33%;" />


## Calculation of Variability

Variability is calculated by overlaying a finer grid over the existing grid and counting the number of tracks in each grid element.  The variability is then defined as:

\[
\text{Variability} = \frac{\text{Standard Deviation of track counts}}{\text{Mean of track counts}}
\]

The figure below illustrates variability for four fictional squares:

![variability](./Pictures/variability.png)

Code reference: [CalculateSquareAttributes.java](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java/blob/main/paint-generate-squares/src/main/java/paint/generatesquares/calc/CalculateSquareAttributes.java)




## Calculation of Diffusion Coefficient (Ext)

The **Diffusion Coefficient** is calculated for each track with ≥3 spots using:

\[
MSD = \frac{1}{n_r \, \text{spots}} \sum_{i=1}^{n_r \, \text{spots}} \left( (x_i - x_0)^2 + (y_i - y_0)^2 \right)
\]

\[
\text{Diffusion Coefficient} = \frac{MSD}{2nt}
\]

Where:
- \(n\) = dimensionality (2 in this case) 
- \(t\) = time interval (0.05 s) 

The **Diffusion Coefficient Ext** compares each spot to the previous one (i-1) instead of the first (0):

\[
MSD = \frac{1}{n_r \, \text{spots}} \sum_{i=1}^{n_r \, \text{spots}} \left( (x_i - x_{i-1})^2 + (y_i - y_{i-1})^2 \right)
\]

\[
\text{Diffusion Coefficient Ext} = \frac{MSD}{2nt}
\]






# Viewer

## Overview

The **Viewer** lets you inspect TrackMate results and determine which squares meet the selection criteria.
The initial dialog resembles the TrackMate plugin, allowing you to select the **Project Root** and **Experiments**. You can also specify the **Images Root** (necessary if you want to replay microscope recordings). Previous Experiment selections (e.g., from TrackMate or Generate Squares) are preserved.

<div style="text-align:center;">
 <img src="./Pictures/viewer-1.png" alt="viewer-1" style="zoom:33%;" />
</div>


## Viewer Layout

Once the Project, Images and Experiment selections have been made, the Viewer dialog opens with three panels:

- **Left**: attributes of the current recording (criteria such as Min Density Ratio, Max Variability, Min R², Neighbour Mode).
- **Center**: TrackMate image (left) and brightfield image (right). 
- **Right**: display controls (borders, shading, labels, square numbers).

![viewer-2](./Pictures/viewer-2.png)


## Filter Recordings

The **Filter Recordings** dialog restricts loaded recordings to specific criteria (e.g., cell type, probe, concentration).  
- Use **Filter** to apply a selection. 
- **Reset** removes a single filter; **Reset All** clears all filters.
- The **Apply** button updates the Viewer with the chosen subset.

![filter-recordings-dialog](./Pictures/filter-recordings-dialog.png)


## Select Squares

The **Select Squares** dialog allows dynamic adjustment of square selection criteria via sliders.
Settings can be applied to:
- The current recording, 
- All recordings in the same experiment, or
- All recordings currently loaded. 

Selections are saved in a **Recording Override** file under the Viewer directory in the Project Root.

When the **Overrides** checkbox (left bottom of the main dialog) is clicked, previously made square selections are applied.

<img src="./Pictures/select-squares-dialog.png" alt="select-squares-dialog" style="zoom:33%;" />


## Assign Cells

The **Assign Cells** dialog lets you group squares into cells (1–6) or mark them unassigned.  
- Select squares by dragging with the mouse.
- Click **Assign** to confirm.

Assignments are saved in a **Square Override** file under the Viewer directory.

When the **Overrides** checkbox (left bottom of the main dialog) is clicked, previously made cell assignments are applied.

<div style="text-align:center;">
<img src="./Pictures/assign-cells-dialog.png" alt="assign-cells-dialog" style="zoom:33%;" />
</div>


## Play Recordings

The Viewer can replay original microscope recordings used by TrackMate. 
- Requires the **Images Root** to be set and images available. 
- A 2,000-frame recording is ~1 GB and may take a few seconds to load. 
- Playback speed is user-selectable.

<img src="./Pictures/play-recordings.png" alt="play-recordings" style="zoom:33%;" />


## Export Image

The **Export Image** option saves the current view as a high-resolution RGB `.png` file.



## Show Squares

The **Show Squares** option displays the square data of the currently displayed recording in your computer's default viewer for `.csv` files.



# Get Omero

When images are downloaded from **Omero**, each recording is placed in its own directory:

<img src="./Pictures/omero-download.png" alt="omero-download" style="zoom:33%;" />

For use in the Glyco-PAINT pipeline, all files must be collected into a single directory. 
The **Get Omero** utility automates this process:

1. Start the utility and select the folder containing the Omero downloads (e.g. `~/Downloads/Omero Folder`). 
2. Press **Process**. 
3. If *Fileset* directories are found, the utility copies all files into a single directory. 
4. Move this directory into the **Images Root** and assign a proper **Experiment** name.

<img src="./Pictures/omero-result.png" alt="omero-result" style="zoom:33%;" />


# Create Experiment

The **Create Experiment** utility helps you set up an *Experiment Info* file. 
This file contains the metadata and instructions required to run the pipeline.

<img src="./Pictures/create-experiment.png" alt="create-experiment" style="zoom:33%;" />

Apply a Regex to select the files of interest. The Recordings will be derived from the name of the 'films'. The Brightfield files are expected to be present, but do not need to be additionally selected. 

A skeleton Experiment Info file is created in the specified Project Root in an Experiment that has the name of the directory in which the images are contained.

Additional information needs to be provided in the skeleton file. An example of a file is shown below.

- **Experiment Name** is a text string.
- **Recording Name** is a text string. It is convenient to have Recording Names that contain the experiment name, but it is not required.
- **Condition Number** is an integer ranging from 1 to the number of unique conditions in the experiment. All Recordings with a given Condition Number, need to have identical Probe Name, Probe Type, Cell Type, Adjuvant and Concentration.  
- **Replicate number** is an integer ranging from 1 to the number of replicates for a given condition. 
- **Probe Name**, **Probe Type**, **Cell Type**, and **Adjuvant** are text strings.
- **Process Flag** needs to contain a value that can be interpreted as a boolean. Uppercase or lower case is supported.
- **Concentration** is expected to be a Double.
- **Threshold** is expected to be a Double.

<img src="./Pictures/experiment-info.png" alt="experiment-info" style="zoom:33%;" />



# Analysis in R

The result of the Glyco-PAINT pipeline is a set of CSV files in the **Project Root**:

- **Squares** 
- **Tracks** 
- **Recordings**

These files contain experiment metadata and calculation results.  They can be imported into **RStudio** (or any other analysis environment) for statistical processing and visualization.

The data in the Squares, Tracks, and Recordings files are a compilation from the last time you ran Generate Squares (either directly or through the TrackMate plugin). You can verify manually by, for example, opening the Recording file in Excel, apply a filter and see which Experiments are included. Per Experiment only the recordings are included for which the Process Flag was set to True.

The values you selected for Min Required Density Ratio, Min Required R Squared, Max Allowable Variability and Neighbour mode are incorporated in the Recordings file. The changes you may have made in the Viewer with the **Selecting Squares** or **Assign Cells** options are not included in the data, but stored in separate **Square Override** and **Recording Override** files (in the Project Root) yet and still need to be incorporated.  A separate command line utility is available for that.




# Development Tools

## JavaDoc

A complete technical reference of the Glyco-PAINT Java codebase is available through [Javadocs](https://leiden-chemical-immunology.github.io/Glyco-PAINT-Java/apidocs/index.html). 
The site lists all public classes, methods, and data types, along with documentation comments from the source code.

- Intended for developers and advanced users. 
- Provides API-level detail and package relationships. 
- Does not replace the user manual or tutorials.

![javadoc](Pictures/javadoc.png)


## GitHub

The full source code is hosted on [GitHub](https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java).  
The project is organized into multiple modules:

- **paint-create-experiment** — Create Experiment utility
- **paint-fiji-plugin** — FiJi plugin
- **paint-generate-squares** — Generate Squares utility 
- **paint-get-omero** — Get Omero utility
- **paint-viewer** — Viewer application 

Supporting modules include:

- **paint-shared-utils** — shared code across applications 
- **shell-scripts** — maintenance scripts 
- **reference-case** — CSV files linked to reference images 
![github](./Pictures/github.png)


## JetBrains IntelliJ IDEA

Development is done in **IntelliJ IDEA**, a popular Java IDE. 
Download from the [IntelliJ IDEA site](https://www.jetbrains.com/idea/). 
Academic users can obtain free professional licenses via the [Student Pack](https://www.jetbrains.com/academy/student-pack/).

![idea](./Pictures/idea.png)

To review or experiment with the code:
1. Install IntelliJ IDEA. 

2. Select **File → New → Project from Version Control**. 

3. Enter the repository URL:

   https://github.com/Leiden-chemical-immunology/Glyco-PAINT-Java

4. Choose a local folder for the project.

With a clone, you can browse, debug, and modify the code locally. 
Refreshing with the latest changes is done via **Git → Pull**. 
Uploading changes requires special authorization.




## RStudio

Analysis of Glyco-PAINT results can be performed in many environments. 
**RStudio** is particularly convenient for statistical analysis and visualisation. 
Download from the [RStudio site](https://posit.co/download/rstudio-desktop/)





## Visual Code Studio

Visual Code Studio is a development environment in its own right, but also a very good text editor. You can download it from https://code.visualstudio.com. Visual Code is handy for editing csv files and provides syntax checking for JSON files.





# Known issues



## Fiji crashes on wakeup on Mac 

On macOS there is a known issue with Fiji crashing on wake up of a computer when a long Fiji job is running. This is caused by macOS' aggressive power management affecting the GPU, OpenGL, and UI threads, the use by Fiji of Java 8.

The workaround is to prevent macOS to engage in the power management. A simple native macOS, SleepKeeper will prevent this. The app can be downloaded from the Paint distribution, but is not signed and macOS will complain about it.

How to open the app the first time:

1. Right-click the app → **Open**
2. macOS will show a warning
3. Click **Open** again
4. After that, the app launches normally

If macOS blocks it outright:

1. Open **System Settings → Privacy & Security**
2. Scroll down to *“SleepKeeper was blocked”*
3. Click **“Allow Anyway”**
4. Try opening the app again



## CSV format on Windows noy compatible 

The default CSV format on Windows is not compatible with the Glyco-PAINT pipeline.

The following steps are necessary to address this:

- Open the Windows Start menu and select the Control Panel
- Open the Regional and Language Options
- Select the Regional options
- Search for List Separator and type a comma
- Confirm the chances 

  
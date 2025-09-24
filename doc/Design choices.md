

# Introduction

A Paint project contains Experiments; Experiments contain Recordings.

Recordings are made under specific experimental parameters such as cell type, probe, concentration. A unique combination of such parameters define a Condition. For every Condition typically multiple Replicates are made. 

On the computer a Project Root is a directory and Experiments are subdirectories.  In a Project Root, a configuration file called 'Paint Config.json' needs to be present. It is generated automatically if it not already exists. The experiment directories as a minimum contain an 'Experiment Info.csv' file, in which the recordings are listed together with relevant metadata.

In addition to a Project Root, an Images Root needs to be present. In the Images Root the recordings (from the microscope) are stored.

For each Experiment the following information is supplied in the 'Experiment Info.csv' file:

- Recording Name, a simple string without spaces.
- Condition Number, a sequence
- Replicate Number
- Probed Name
- Probe Type
- Cell Type
- Adjuvant
- Concentration
- Process Flag
- Threshold

  

For all Paint operations a Project Root needs to be specified. Once a project has been selected the user selects the Experiments and starts the operation.  



# TrackMate

The Fiji TrackMate plugin is used to detect spots in the recordings and to create tracks.

After having selected a Project Root, the user specifies the Experiments for which processing is required and an Images Root. The following checks are made:

- Does the Project Root exist?
- Are the Experiments that are selected by the user valid, i.e. are 'Experiment Info.csv' files present?
- Are the  'Experiment Info.csv' files in the required format?
- Does the Images Root exist?
- Is for each selected Experiment a directory present in the Images Root with a corresponding name?
- Is for each Recording in the 'Experiment Info.csv' files a corresponding image file present in the corresponding directory?

If all conditions are met, processing can start, otherwise the plugin exits. Default parameters are read from the 'Paint Configuration.json' file, where the user can select meaningful values. If the 'Paint Configuration.json' file is not present, a default version is created. 

For every recording, the threshold value is specified by the user in the 'Experiment Info.csv' file. Only recordings for which the Process Flag is set to True are processed. 



"TrackMate": {

​    "MAX_FRAME_GAP": 3,

​    "ALTERNATIVE_LINKING_COST_FACTOR": 1.05,

​    "DO_SUBPIXEL_LOCALIZATION": false,

​    "MIN_NR_SPOTS_IN_TRACK": 3,

​    "LINKING_MAX_DISTANCE": 0.6,

​    "MAX_NR_SPOTS_IN_IMAGE": 2000000,

​    "GAP_CLOSING_MAX_DISTANCE": 1.2,

​    "TARGET_CHANNEL": 1,

​    "SPLITTING_MAX_DISTANCE": 15.0,

​    "TRACK_COLOURING": "TRACK_DURATION",

​    "RADIUS": 0.5,

​    "ALLOW_GAP_CLOSING": true,

​    "DO_MEDIAN_FILTERING": false,

​    "ALLOW_TRACK_SPLITTING": false,

​    "ALLOW_TRACK_MERGING": false,

​    "MERGING_MAX_DISTANCE": 15.0

  },



The selected Experiments in the Project Root (identified by their directory names), need to be also present in the Images Root.

- For TrackMate processing the user selects an 'images root'.
- The experiments found under a project need to be present as directories in the images root. 
- The user only ever selects a project, directly selecting an experiment is not possible.


	1.	Generate Squares as an app on your desktop.
	2.	Run TrackMate simply by dragging a JAR into the plugin directory.
	3.	User interface of the TrackMate plugin and Generate Squares identical.
	4.	No need to compile anymore.
	5.	Extensive checks on file formats, making a crash due to incorrect input less likely.
	6.	More flexible paint.json files (one per project).
	7.	No need to generate Brightfields anymore.
	8.	You can name your projects as strangely as you like 🙂
	9.  An average user no longer needs a development environment.
	10. Code documentation generated autpmatically

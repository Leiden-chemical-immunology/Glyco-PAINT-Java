The Glyco-PAINT pipeline is a research tool for processing images.

The pipeline is a set of software tools that operate on microscopy recordings to extract data.



Central in the pipeline is the 'Project Root', a directory containing subdirectories that represent Experiments.  Experiment directories contain as a minimum an Experiment Info file that contains information about Recordings of the Experiment.

Parallel to the Project Root, the pipeline depends on an 'Images Root' directory, where the microscopy data is stored. The Experiment structure in the Project Root needs to be exactly replicated in the Images Root, so for example, if under the Project Root, Experiments 'Exp A', 'Exp B' and 'Exp C' exist,  those same directories are expected to be present under the Images Root. The Experiment directories in the Images Root contain the files recorded with the microscope. For every recording two files are needed: the multi-frame recording, typically 2000 frames per recording and a single-frame Bright Field image. 

The images are likely downloaded from Omero (for which the pipeline has a Get Omero app) or may be copied in any other way. The names of the recordings can be chosen freely, with the restriction that the name of the Brightfield image is equal to the multi-frame recording with '-BF' attached.

In the Project Root directory, a Paint Configuration file exists, in which important parameter for the pipeline are stored. If the file does not exist, for example in a new installation, it will be created automatically with default parameters.

The various Glyco-PAINT tools write information into a Console and also to a log file, which is stored in the Log directory that will be created  in the Project Root directory.

The pipeline flow is as follows:

1. Ensure the images are stored in a named directory in the Images Root. The name of the directory will become the name of the Experiment. The 'Get Omero' utility may be handy in this stage.  
2. Run the 'Create Experiment' utility to create an Experiment under the Project Root. The utility lets the user select the image files that become Recordings in the experiment. An Experiment Info file is created. The user needs to supply metadata to describe characteristics of the experiment.
3. With thr Experiment Info file selected, the Glyco-PAINT plugin in Fiji can be run. The user can select which Experiments are to be processed. Spot detection and path generation is performed and the results are summarised in the All Tracks and Recordings files.
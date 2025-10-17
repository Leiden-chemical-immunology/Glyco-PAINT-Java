The Glyco-PAINT pipeline is a research tool for processing images.

The pipeline is a set of softwarev tools that operate on microscopy recordings to extract data.



Central in the pipline is the Project Root, a directory containing directories that represent Experiments.  Experiment directories contain as a minimum an Experiment Info file that contains information about Recordings of the Experiment.

Parallel to the Project Root, the pipline depends on an Images Root directory, where the microscopy data is stored. The Experiment structure in the Project Root needs to be exactly replicated in the Images Root, So for example, if under the Project Root, Experiments 'Exp A', 'Exp B' and 'Exp C' exist,  those same directories are expected to be present under the Images Root. The Experiment directories in the Images Root contain the files recorded with the microscope. For every recording two files aare needed: the multi-frame recording, typically 2000 frames per recording and a single -rame Bright Field image. The images are likely downloaded from Omero (for which the pipeline has a Get Omero app) or may be copied in any other way. The names of the recordings can be chosen freely, with the restriction that the name of the Brightfield image is equal to the multi-frame recording with '-BF' attached.

In the Project Root directory, a Paint Configuration file exists, in which important parameter for the pipeline are stored. If the file does not exist, for example in a new installation, it will be created automatically with default parameters.

The various Glyco-PAINT tools write information into a Console and also to a log file, which is stored in the Log directory that will be created  in the Project Root directory.
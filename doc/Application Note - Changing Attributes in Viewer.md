
<h1 style="text-align:center; margin:0; font-size:2.2em;">Application Note</h1>



<h3 style="text-align:center; margin:0;">Changing Recording attributes in the Viewer</h3>




### Introduction

The Glyco-PAINT pipeline is developed to generate data files (**Recordings**, **Squares**, **Tracks**)  ready for analysis, without any manual intervention. 

The process flow from beginning to end is to run the **TrackMate Batch** plugin on the selected series of experiments and then to run **Generate Squares** on the same selection of Experiments.

In each of the Experiment directories new **Recordings**, **Squares**, **Tracks** files will have been created. In the root of the Project aggregated versions ofof these files contain the information on the recordings from all selected experiments. It is normally this version of the files that you use for further analysis. In this set of files, no recordings have been excluded, the standard selection criteria are applied to all squares and no squares have been allocated to cells.

It is possible to change attributes of recordings in the Viewer. This note describes that process.



### Using the Viewer to change recordings data 

With the Viewer it is possible it is possible to change data in a few ways.

- Recordings can be excluded, if the quality is deemed insufficient.	
- The defaults Square selection criteria can be overruled for selected recordings.
- Squares can be assigned to seperate cells.

The procedure for making changes is explained in the following: 

- **Excluding Recordings**. You can exclude a Recording, by clicking on the **Exclude Recording** button. In the Viewer the recording is marked as Excluded (in red text). For that recording the button text changes to 'Include Recording'. The Viewer writes a record in the 'Recording Exclude' file, in the Viewer directory under the project root. 

- **Changing Square Selection**.  The square selection criteria can  be changed for a Recording or groups of Recordings, by clicking the **Select Squares** button. If you apply changes you have made, the Viewer writes records in the 'Recording Override file, in the Viewer directory under the project root. 

- **Assigning Squares to Cells**. Cell assignments  can be made by clicking the **Assign Cells** button. The Viewer writes records in the 'Squares Override file, in the Viewer directory under the project root.  



### Viewing previously made changes

If you open the Viewer a subsequent time, any changes made previously are not automatically shown. To see these changes you need to click the **Overrides** checkbox at the left bottom of the dialog. The information in the three change files will then be applied and showmup in the Viewer.

Note that the affect of changes is cumulative, e.g. you may have assigned squares to cells, but if you have subsequently changed filter criteria, it is possible that thoise changes are no longer visible.



#### Applying changes to the  **Recordings**, **Squares** and **Tracks** files

When you exit the Viewer, you have an option the save the changed made for later analysis. You do this by ensurring that  **Overrides** checkbox is clicked, before you exit.

The Viewer will then generate alternative versions of the **Recordings**, **Squares** and **Tracks** files in which the changes have been incorporated. These files have the postfix '**-override**'.

The changes made are as following: 

- **Excluding Recordings**. The **Exclude** field in **Recordings-override** is set to true for the excluded recordings. All the information on the excluded recordings is removed from **Squares-override** and **Tracks-override**.
- **Changing Square Selection**.  The fields that have been changed are updated in **Recordings-override**. In **Squares-override** the Visible fields of all Squares for the affacted recordings are updated. 
- **Assigning Squares to Cells**. In the **Squares-override** file the Cell-Id field of affected Squares is updated.

Important is thgat the original **Recordings**, §**Squares** and **Tracks** files are never changed. If you have made changes and have stored them, for furtyher analysis you need to work wih the **Recordings-override**, **Squares-override** and **Tracks-override**  versions.

##### Perform a parameter sweep on  Experiment 'SweepExperiment' in Project 'SweepProject 



User:

- Prepares  a sweep file and put it in the project directory (for format see below)



Software

- Check if a sweep.json file is present in the project directory.
- Look in the sweep section for which parameters need to be swept.
- Create a 'Sweep' directory in the project
- For each parameter cycle through the values lsited in the sweep file section 
- Update the JSON file with the current paramneter
- Perform the TrackMate calculations 
- Write the All Recordings and Tracks file into seperate directories for each case
- Add a Case field to the files describing the case 'Threshold 5', 'Threshold 6' 



​	SweepProject

​		SweepExperiment

​			Sweep

​				Threshold 5

​				Threshold-6

 

- Run Generate Squares on all the Sweep directories (requires a small change in Generate Squares)
- Compile All Recordings, All Squares











 









{  
  "Sweep": {
    "Threshold": true,
    "Min Required R Squared": false,
    "Min Tracks to Calculate Tau": false,
    "Fraction of Squares to Determine Background": false,
    "Number of Squares in Row": false,
    "Exclude zero DC tracks from Tau Calculation": false,
    "Max Allowable Variability": false,
    "Min Required Density Ratio": false,
    "Number of Squares in Column": false
  },

  "Threshold": {
    "Value 0": 5,
    "Value 1": 6,
    "Value 2": 7,
    "Value 3": 8,
    "Value 4": 9,
    "Value 5": 10,
    "Value 6": 11,
    "Value 7": 12,
    "Value 8": 13
  },
  "Min Required R Squared": {
    "Value 0": 0.1,
    "Value 1": 0.2,
    "Value 2": 0.3,
    "Value 3": 0.4,
    "Value 4": 0.5,
    "Value 5": 0.6,
    "Value 6": 0.7,
    "Value 7": 0.8
  },
  "Min Tracks to Calculate Tau": {
    "Value 0": 5,
    "Value 1": 10,
    "Value 2": 15,
    "Value 3": 20,
    "Value 4": 25
  }

}


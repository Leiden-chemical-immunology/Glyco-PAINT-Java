library(readxl)
source('../../Utility Code/Eliminate.R')


read_data <- function(root_directory, output, manually_exclude = FALSE, duration_exclude = FALSE, exclude_file='Excluded Images.csv', perc_long_tracks) {
  
  
  squares_file        <- 'All Squares.csv'
  images_file         <- 'All Recordings.csv'
  tracks_file         <- 'All Tracks.csv'
  
  exclude_file        <- 'Excluded Recordings.csv'
  valid_squares_file  <- 'Valid Squares.csv'
  valid_images_file   <- 'Valid Recordings.csv'

  squares_master <- read_csv(paste0(root_directory, '/', output, '/', squares_file), show_col_types = FALSE)
  images_master  <- read_csv(paste0(root_directory, '/', output, '/', images_file), show_col_types = FALSE)
  tracks_master <- read_csv(paste0(root_directory, '/', output, '/', tracks_file), show_col_types = FALSE)
  
  #Adjust column names in tracks master
  tracks_master_only <- tracks_master
  
  # Remove spaces from column names
  names(squares_master) <- str_replace_all(names(squares_master), c(" " = "_" ))
  names(images_master)  <- str_replace_all(names(images_master), c(" " = "_" ))
  names(tracks_master)  <- str_replace_all(names(tracks_master), c(" " = "_" ))
  
  # Eliminate excluded images
  data <- eliminate_excluded(root_directory, squares_master, images_master, manually_exclude, duration_exclude, exclude_file)
  squares_master <- data$squares_master
  images_master  <- data$images_master

  # Check if Valency and Structure exist and create if needed
  if (!('Valency' %in% names(squares_master))) {
    
    # Suppressing both messages and warnings within the code block
    squares_master <- suppressMessages(suppressWarnings(
      squares_master %>%
        separate(Probe, into = c('Valency', 'Structure'), sep = ' ', remove = FALSE) %>%
        mutate(Structure = replace_na(Structure, 'Not specified')) %>%
        mutate(Valency = replace_na(Valency, 0))
    ))
  }
  
  # Set Structure to Control when Probe is Control
  squares_master$Structure[squares_master$Probe == 'Control'] <- 'Control'
  squares_master$Valency[squares_master$Probe == 'Control']   <- 'Control'

  # Shorten Cell_Type names
  squares_master$Cell_Type[squares_master$Cell_Type == 'CHO-MR']  <- 'CHOMR'
  squares_master$Cell_Type[squares_master$Cell_Type == 'MR -/-']  <- 'MR-'
  
  images_master$Cell_Type[images_master$Cell_Type == 'CHO-MR']  <- 'CHOMR'
  images_master$Cell_Type[images_master$Cell_Type == 'MR -/-']  <- 'MR-'
  
  squares_master <- squares_master %>%
    mutate(Threshold = as.integer(str_extract(Ext_Recording_Name, "(?<=-)(\\d+)$")))
  
  # Change Adjuvant name from No to None 
  squares_master$Adjuvant[squares_master$Adjuvant == 'No']       <- 'None'
  images_master$Adjuvant[images_master$Adjuvant == 'No']         <- 'None'
  
  # Correct concentrations (irrelevant accuracy)
  squares_master$Concentration[squares_master$Concentration == 4.9]     <- 5
  squares_master$Concentration[squares_master$Concentration == 14.6]    <- 15
  
  images_master$Concentration[images_master$Concentration == 4.9]     <- 5
  images_master$Concentration[images_master$Concentration == 14.6]    <- 15
  
  # Remove suspect Concentration  
  squares_master <- squares_master %>%
    filter(squares_master$Concentration != 0.1)
  
  images_master <- images_master %>%
    filter(images_master$Concentration != 0.1)
  
  
  # Merge Adjuvants
  squares_master$Adjuvant[squares_master$Adjuvant == 'MPLA']     <- 'Adj'
  squares_master$Adjuvant[squares_master$Adjuvant == 'LPS']      <- 'Adj'
  squares_master$Adjuvant[squares_master$Adjuvant == 'LPS+CytD'] <- 'Adj+CytD'
  squares_master$Adjuvant[squares_master$Adjuvant == 'M2pep'] <- 'M2'
  
  images_master$Adjuvant[images_master$Adjuvant == 'MPLA']     <- 'Adj'
  images_master$Adjuvant[images_master$Adjuvant == 'LPS']      <- 'Adj'
  images_master$Adjuvant[images_master$Adjuvant == 'LPS+CytD'] <- 'Adj+CytD'
  images_master$Adjuvant[images_master$Adjuvant == 'M2pep'] <- 'M2'
  
  # Make columns factor where necessary
  squares_master$Probe      <- factor(squares_master$Probe,       levels = c('1 Mono', '2 Mono', '6 Mono', '1 Bi', '2 Bi', '6 Bi', '1 Tri', '2 Tri', '6 Tri', 'Control'))
  squares_master$Probe_Type <- factor(squares_master$Probe_Type,  levels = c('Simple', 'Epitope'))
  squares_master$Cell_Type  <- factor(squares_master$Cell_Type,   levels = c('CHOMR', 'BMDC', "MR-", "iCD103", "spDC", "BMDM"))
  squares_master$Adjuvant   <- factor(squares_master$Adjuvant,    levels = c('None', 'CytD', 'Adj', 'Adj+CytD', "M1", "M2", "M2pep"))
  squares_master$Valency    <- factor(squares_master$Valency,     levels = c('1', '2', '6', 'Control'))
  squares_master$Structure  <- factor(squares_master$Structure,   levels = c('Mono', 'Bi', 'Tri', 'Control'))
  
  # Make columns factor where necessary
  images_master$Probe      <- factor(images_master$Probe,       levels = c('1 Mono', '2 Mono', '6 Mono', '1 Bi', '2 Bi', '6 Bi', '1 Tri', '2 Tri', '6 Tri', 'Control'))
  images_master$Probe_Type <- factor(images_master$Probe_Type,  levels = c('Simple', 'Epitope'))
  images_master$Cell_Type  <- factor(images_master$Cell_Type,   levels = c('CHOMR', 'BMDC', "MR-", "iCD103", "spDC","BMDM" ))
  images_master$Adjuvant   <- factor(images_master$Adjuvant,    levels = c('None', 'CytD', 'Adj', 'Adj+CytD',"M1", "M2", "M1 + SI", "M2pep"))
  
  
  # Check for NA
  if (FALSE) {
    if (sum(is.na(squares_master)) > 0 ) {
      which(is.na(squares_master), arr.ind=TRUE)
      stop('Na in squares master')
    }
  
    if (sum(is.na(images_master)) > 0 ) {
      which(is.na(images_master), arr.ind=TRUE)
      stop('Na in images_master')
  
    }
  }

  
  # Join tracks_master with images_master to retrieve probe, cell type and adjuvant information
  tracks_master <- left_join(tracks_master, 
                             squares_master %>% select(Ext_Recording_Name, Experiment_Date, Square_Nr, Cell_Type, Probe, 
                                                       Probe_Type, Total_Track_Duration, Density_Ratio,Density, Tau, Adjuvant), 
                             by = c("Ext_Recording_Name", "Square_Nr"))
  
  # Multiply density and diffusion coefficient such that units are correct. Density is evts/µm2/ms and diffusion coefficient is in nm2/ms (or µm2/s)
  squares_master$Density <- squares_master$Density * 1000
  squares_master$Diffusion_Coefficient <- squares_master$Diffusion_Coefficient * 1000
  
  # Process tracks master to add median track parameters per square to the all squares
  avg_track_duration <- tracks_master %>%
    group_by(Square_Nr, Ext_Recording_Name) %>%
    summarise(
      Average_Track_Duration = median(Track_Duration, na.rm = TRUE),
      Avg_DC = median(Diffusion_Coefficient *1000, na.rm = TRUE),
      # Average displacement, velocity
      Avg_Displacement = median(Track_Displacement, na.rm = TRUE),
      Avg_Speed = median(Track_Mean_Speed, na.rm = TRUE),
      Max_Speed = median(Track_Max_Speed, na.rm = TRUE))

  # Append the result to all_squares_df by Square_Nr
  squares_master <- squares_master %>%
    left_join(avg_track_duration, by = c("Square_Nr","Ext_Recording_Name"))

  # Process tracks_master to add average long and average short duration tracks
  avg_track_durations <- tracks_master %>%
    group_by(Square_Nr, Ext_Recording_Name) %>%
    summarise(
      Track_Count = n(),

      # Average duration of long tracks
      Avg_Long_Track_Duration = if (Track_Count < 10) {
        # Take the mean track duration
        median(Track_Duration, na.rm = TRUE)
      } else {
        # Calculate the number of tracks to average
        nr_tracks_to_average = round(perc_long_tracks * Track_Count)
        # Average of the longest tracks
        median(tail(sort(Track_Duration), nr_tracks_to_average), na.rm = TRUE)
      },

      # Average duration of short tracks
      Avg_Short_Track_Duration = if (Track_Count < 10) {
        # Same as long tracks since all tracks are included
        median(Track_Duration, na.rm = TRUE)
      } else {
        # Calculate the number of short tracks
        nr_tracks_to_average = round(perc_long_tracks * Track_Count)
        # Average of the shortest tracks
        median(head(sort(Track_Duration), Track_Count - nr_tracks_to_average), na.rm = TRUE)
      }
    )
  
  # Append the result to all_squares_df by Square_Nr
  squares_master <- squares_master %>%
    left_join(avg_track_durations, by = c("Square_Nr", "Ext_Recording_Name"))
  
  return (list(squares_master=squares_master, images_master=images_master, tracks_master=tracks_master, tracks_master_only=tracks_master_only))
}



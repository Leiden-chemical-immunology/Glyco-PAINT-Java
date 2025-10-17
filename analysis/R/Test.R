
---
title: "Epitope on BMDC comparison of adjuvants CytD and None"
subtitle: 'BMDC - Epitope - CytD and None.Rmd'
fontfamily: sans
output:
  pdf_document:
    toc: yes
    toc_depth: 3
  html_document: default
  word_document: default
---

```{r Libraries, echo=FALSE, warning=FALSE, message=FALSE}

library(tidyverse)
library(readxl)
library(knitr)
library(gridExtra)
library(kableExtra)
library(ggpmisc)
library(ggtext)

```


```{r , echo=FALSE}


data_file_mac <-'Full Results'

```


```{r , echo=FALSE}

read_data <- function(root_directory) {

  # Read the files
  squares_master <- read_csv(paste0(root_directory, '/', 'All Squares.csv'), show_col_types = FALSE)
  recordings_master <- read_csv(paste0(root_directory, '/', 'All Recordings.csv'), show_col_types = FALSE)

  # Remove spaces from column names
  names(squares_master) <- str_replace_all(names(squares_master), c(" " = "_" ))
  names(recordings_master)<- str_replace_all(names(recordings_master), c(" " = "_" ))

  # Multiply density and diffusion coefficient such that units are correct. Density is evts/µm2/ms and diffusion coefficient is in nm2/ms (or µm2/s)
  squares_master$Density <- squares_master$Density * 1000
  squares_master$Mean_Diffusion_Coefficient <- squares_master$Mean_Diffusion_Coefficient * 1000
  squares_master$Median_Diffusion_Coefficient <- squares_master$Median_Diffusion_Coefficient * 1000

  # Only consider recordings:
  #   that have been processed because the process flag was  set
  #   that did not contain too many spot
  #   that have expected lengths (2000 frames)

  recordings_master <- recordings_master %>%
    filter(Process == 'Yes',
           Nr_Tracks != -1,
           Recording_Size >= 1058000896 * 0.95 & Recording_Size <=  1058000896 * 1.20)

  # Only leave data in All Squares and All Tracks that corresponds with what is in All Recordings
  squares_master <- squares_master %>%
    filter(Ext_Recording_Name %in% recordings_master$Ext_Recording_Name)

  # Create Valency and Structure in squares master. This only works for Regular probes
  if (!('Valency' %in% names(squares_master))) {

    # Suppressing both messages and warnings within the code block
    squares_master <- suppressMessages(suppressWarnings (
      squares_master %>%
        separate(Probe, into = c('Valency', 'Structure'), sep = ' ', remove = FALSE) %>%
        mutate(
          Structure = replace_na(Structure, 'Not specified'),
          Valency = if_else(Structure == "Not specified", "Not specified", Valency)
        )
    ))
  }

  # Set Structure to Control when Probe is Control
  squares_master$Structure[squares_master$Probe == 'Control'] <- 'Control'
  squares_master$Valency[squares_master$Probe == 'Control']   <- 'Control'

  # Remove the threshold part of the image name and store in column Recording_Name
  squares_master <- squares_master %>%
    mutate(Ori_Recording_Name = Ext_Recording_Name) %>%
    separate_wider_regex(Ext_Recording_Name, c(Recording_Name = ".*", "-threshold-\\d+")) %>%
    mutate(Ext_Recording_Name = Ori_Recording_Name)

  # Remove the '-' from CHO-MR, because that name is used later as a column name (and that give sproblems)
  squares_master$Cell_Type[squares_master$Cell_Type == 'CHO-MR']  <- 'CHOMR'
  recordings_master$Cell_Type[recordings_master$Cell_Type == 'CHO-MR']  <- 'CHOMR'

  # fix the MR -/- notation for bmdc and bmdm
  squares_master$Cell_Type[squares_master$Cell_Type == 'BMDM MR -/-']  <- 'BMDMMRko'
  recordings_master$Cell_Type[recordings_master$Cell_Type == 'BMDM MR -/-']  <- 'BMDMMRko'

  squares_master$Cell_Type[squares_master$Cell_Type == 'MR -/-']  <- 'BMDCMRko'
  recordings_master$Cell_Type[recordings_master$Cell_Type == 'MR -/-']  <- 'BMDCMRko'

  # Change Adjuvant name from No to None
  squares_master$Adjuvant[squares_master$Adjuvant %in% c('No', '', NA)] <- 'None'
  recordings_master$Adjuvant[recordings_master$Adjuvant %in% c('No', '', NA)] <- 'None'

  # Change adjuvant from None to M0 only for BMDM
  squares_master$Adjuvant[squares_master$Adjuvant == 'None' & squares_master$Cell_Type == 'BMDM'] <- 'M0'
  recordings_master$Adjuvant[recordings_master$Adjuvant == 'None' & recordings_master$Cell_Type == 'BMDM'] <- 'M0'

  # Correct concentrations (irrelevant accuracy)
  squares_master$Concentration[squares_master$Concentration == 4.9] <- 5
  squares_master$Concentration[squares_master$Concentration == 14.6] <- 15

  recordings_master$Concentration[recordings_master$Concentration == 4.9] <- 5
  recordings_master$Concentration[recordings_master$Concentration == 14.6] <- 15

  # Remove suspect Concentration (question is whether we really used it)
  squares_master <- squares_master %>%
    filter(squares_master$Concentration != 0.1)
  recordings_master <- recordings_master %>%
    filter(recordings_master$Concentration != 0.1)

  # Make Concentration integer
  squares_master$Concentration <- as.numeric(squares_master$Concentration)

  # Make columns factor where necessary
  probe_factor = c('1 Mono', '2 Mono', '6 Mono', '1 Bi', '2 Bi', '6 Bi', '1 Tri', '2 Tri', '6 Tri', 'Control')
  probe_type_factor = c('Simple', 'Epitope')
  cell_type_factor = c('BMDC', 'CHOMR', 'BMDCMRko', 'iCD103', 'spDC', 'BMDM', 'BMDMMRko', 'CHO')
  adjuvant_factor = c('CytD', 'None', 'LPS', 'LPS+CytD', 'MPLA', 'M1', 'M2', 'M0', 'M1 + SI', 'M2pep')
  valency_factor = c('1', '2', '6', 'Control')
  structure_factor = c('Mono', 'Bi', 'Tri', 'Control')

  squares_master$Probe      <- factor(squares_master$Probe,       levels = probe_factor)
  squares_master$Probe_Type <- factor(squares_master$Probe_Type,  levels = probe_type_factor)
  squares_master$Cell_Type  <- factor(squares_master$Cell_Type,   levels = cell_type_factor)
  squares_master$Adjuvant   <- factor(squares_master$Adjuvant,    levels = adjuvant_factor)
  squares_master$Valency    <- factor(squares_master$Valency,     levels = valency_factor)
  squares_master$Structure  <- factor(squares_master$Structure,   levels = structure_factor)

  # Make columns factor where necessary
  recordings_master$Probe      <- factor(recordings_master$Probe,       levels = probe_factor)
  recordings_master$Probe_Type <- factor(recordings_master$Probe_Type,  levels = probe_type_factor)
  recordings_master$Cell_Type  <- factor(recordings_master$Cell_Type,   levels = cell_type_factor)
  recordings_master$Adjuvant   <- factor(recordings_master$Adjuvant,    levels = adjuvant_factor)

  # Ensure Median Filtering is Boolean
  recordings_master$Median_Filtering <- as.logical(recordings_master$Median_Filtering)


  write_csv(squares_master, '~/Downloads/squares_master_processed.csv')

  return (list(squares_master=squares_master, recordings_master=recordings_master))
}

```


```{r , echo=FALSE}

# Function to load data for a specific number of squares
load_data <- function(data_file, num_squares) {
  data_file <- paste0(data_file, '/', num_squares)
  print(data_file)
  data <- read_data(data_file)
  return(data)
}
```


```{r , echo=FALSE}

# List of squares to load
squares <- c(5, 10, 20, 30, 40, 50)

# Load data for each square count and store in a named list
datasets <- lapply(squares, function(num_squares) {
  load_data(data_file_mac, num_squares)
})

# Assign names to the datasets list for easy reference
names(datasets) <- paste0("data", squares)


```

```{r echo=FALSE}


# Datasets for each square count
datasets_for_plots <- lapply(squares, function(num) datasets[[paste0("data", num)]]$squares_master)

recording_datasets_for_plots <- lapply(squares, function(num) datasets[[paste0("data", num)]]$recordings_master)
dataset_titles <- paste0("Squares in row ", squares)

# Generate all plots

```


```{r echo=FALSE, density_significance_bmdc_adj, fig.fullwidth=TRUE, fig.height=6, fig.width=8, warning=FALSE}

# Main function to combine other metrics and plot
create_metrics_plot <- function(datasets_for_plots, squares) {
  # Combine datasets and reshape for other metrics (excluding Tau_Percentage)
  all_metrics_data <- bind_rows(
    lapply(seq_along(datasets_for_plots), function(i) {
      datasets_for_plots[[i]] %>%
        mutate(Squares = squares[i]) %>%
        filter(Cell_Type %in% c('BMDC'),

               #Recording_Name == '221101-Exp-4-A3-1',
               #Recording_Name == '240116-Exp-1-A1-3', # BDMC example
               #Adjuvant == 'None',
              # Probe_Type == 'Epitope',
               # Probe == '6 Tri',
               R_Squared > 0,
               #Density_Ratio > 2,
              Tau > 0
              ) %>%
        pivot_longer(
          cols = c("R_Squared", "Tau", "Density_Ratio", "Variability", "Density"),
          names_to = "Metric",
          values_to = "Value"
        )
    })
  )

  return(all_metrics_data)
}

# Combine all metrics, including Tau_Percentage, into a final dataset
# Process Tau_Percentage first
#tau_percentage_data <- create_tau_percentage_plot(recording_datasets_for_plots, squares)
# Process other metrics
all_metrics_data <- create_metrics_plot(datasets_for_plots, squares)

# Define custom y-axis limits for each metric
metric_y_limits <- list(
  R_Squared = c(0, 1),
  Tau = c(0, 300),
  Density_Ratio = c(0, 80),
  Variability = c(0, 10),
  Density = c(0, 30)
)
td <- all_metrics_data %>%
  filter( Recording_Name == '240116-Exp-1-A1-3')


# Define metric labels for the plot
metric_labels <- c(
  R_Squared = expression(R^2),
  Tau = expression(Tau),
  Density_Ratio = "Density Ratio",
  Variability = "Variability",
  Density = "Density"
)

# Add a column to assign y-limits for each metric
all_metrics_data <- all_metrics_data %>%
  mutate(
    Y_Limit_Min = map_dbl(Metric, ~ metric_y_limits[[.x]][1]),
    Y_Limit_Max = map_dbl(Metric, ~ metric_y_limits[[.x]][2])
  )

# Split the data by metric and adjust y-limits individually
# One plot per metric
plots <- lapply(names(metric_y_limits), function(metric) {
  limits <- metric_y_limits[[metric]]

  ggplot(filter(all_metrics_data, Metric == metric),
         aes(x = factor(Squares), y = Value)) +
    geom_boxplot(outlier.shape = NA, position = position_dodge(width = 0.8)) +
    coord_cartesian(ylim = limits) +
    labs(
      x = "Squares",
      y = metric_labels[[metric]]  # <-- metric label on y-axis
    ) +
    theme_light(base_size = 10) +
    scale_y_continuous(expand = c(0, 0)) +
    theme(
      panel.border = element_blank(),
      panel.grid.major = element_blank(),
      panel.grid.minor = element_blank(),
      axis.line = element_line(size = 0.25, colour = 'black'),
      axis.text.x = element_text(size = 9, angle = 45, hjust = 1),
      legend.position = "none"
    )
})

# Combine into a grid
combined_metric_plot <- cowplot::plot_grid(plotlist = plots, ncol = 2, align = "v")
combined_metric_plot


```

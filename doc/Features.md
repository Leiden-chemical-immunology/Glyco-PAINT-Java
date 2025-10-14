# Release notes PAINT-App in Java



1. **Fully Rewritten in Java for Improved Performance**

   All core logic has been reimplemented in Java, replacing older scripting components. This shift enables more efficient execution and better integration with modern Java tooling.

2. **Complete Redesign for Maintainability and Clarity**

   The system has been refactored from the ground up, removing legacy artifacts and outdated designs. The result is a more coherent, maintainable, and future-proof codebase.

3. **Modern Plugin Architecture with Fiji Update Support**

   The PAINT Fiji plugin is now a standard Java plugin, distributed via two JAR files. It supports automatic updates through Fiji’s built-in update mechanism, simplifying installation and maintenance for end users.

4. **Standalone Applications for Key Tools**

   Tools such as *Generate Squares* are now delivered as self-contained desktop applications, runnable independently of Fiji or any development environment.

5. **Clear Separation of User and Developer Roles**

   End users no longer need access to development tools. A clean separation between usage and development streamlines deployment. Full source code remains openly available via [GitHub](https://github.com/) for transparency and contribution.

6. **Extensive Documentation via JavaDoc**

   The entire codebase is thoroughly documented using the JavaDoc system, offering detailed insights into each component’s purpose and usage.

7. **Streamlined Workflow**

   The user workflow has been significantly simplified and now follows three straightforward steps:

   - Run the TrackMate plugin
   - Generate Squares
   - Analyze data in R (or another statistical environment)

8. **Unified, Simplified User Interface**

   The graphical user interface is now consistent across all tools, including the TrackMate plugin and the Generate Squares app, reducing the learning curve and improving user experience.

9. **Configurable via Project-Specific Settings**

   Flexibility is enhanced by allowing each project to define its own configuration using Paint Configuration files, enabling fine-tuned, reproducible analysis pipelines.

10. **Optimized Data Formats**

    CSV file formats have been simplified by removing obsolete fields and reducing redundancy. This leads to file sizes up to 50% smaller compared to previous versions.

    ⚠️ *Note: existing analysis scripts (e.g., in R) require updates to accommodate the new format.*

11. **Robust Pre-Execution Validation**

    Comprehensive validation mechanisms detect potential configuration or data issues before execution begins, reducing runtime failures and improving reliability.

12. **Defensive Programming for Fast Error Resolution**

    Extensive error checking and structured exception handling ensure that runtime problems are clearly reported, facilitating efficient debugging and support




#  Development Log



#### Paint Apps as Desktop App

- All Paint Apps are standalone and can be started as any macOS application. 
- The standard Apple app package structure is used.



#### Paint Config file sits in the project directory

- Each project has its own configuration file. 
- When no file is print, one is created with default parameters



#### Logs file sits in the project directory

- Log file are no longer kept in a central place, but stored project specifically.
- In the project directory a Logs directory  will be created if it does not exist.
- With each run a sequence number for the log will be increased. 

 

#### PaintLogger uses a console window dedicated to Paint

- The Paint plugin and apps use its dedicated Console window (previously the Fiji plugin used Fiji's console and Generate Squares did not have a console).
- The Fiji console is blocked. The heavy dump of Fiji messages and AWT stack dumps are now hidden: considerably less distraction.
- All messages are shown in the console and additionally stored in the log file.
- The Logger supports four levels of messages: debug, info, warning and error, The logging level be set so that only 'equal and higher' message are displayed.
- Info and debug messages are coloured in black, warning and error messages are amber and red. 
- Console can be closed, saved and auto scrolling can be (to allow the user to inspect a section higher up).



#### Upfront validation

- Before TrackMate or Generate Squares start, input file correctness is validated.
- Presence of the expected headers is validated.
- Presence of incorrect datatypes in columns is detected.
- Inconsistency in Condition parameters is detected.
- Absence of necessary image files is detected.
- Clear, specific indications of the error help quick corrections.



#### Version information

- Version information and generation data of jars are reported in the PaintLog to help detect version problems.




#### TrackMate progress

- TrackMate now runs in a separate thread and is given time slices of 3 seconds allowing the printing of progress dots (in the Console). User has visual conformation that TrackMate is still alive.



#### Sweep support - under development

- Support for parameter sweeps is implemented allowing TrackMate and Generate Squares calculations to be performed over a range of parameters.
- A Sweep Config.csv file in the project directory specifies the sweep parameters and range.


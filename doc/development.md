**Find all the jar file with paint in the name**
find / -type f -name "*paint*.jar" 2>/dev/null

**Find all the jar files in the development environment with paint in the name**
find /users/Hans/JavaPaintProjects -type f -name "*paint*.jar"
find /users/Hans/.m2 -type f -name "*paint*.jar"

**Find all the jar files in the development environment with paint in the name** with date
find /Users/Hans/JavaPaintProjects -type f -name "*paint*.jar" -exec stat -f "%Sm %N" {} \;
find /users/Hans/.m2 -type f -name "*paint*.jar" -exec stat -f "%Sm %N" {} \;

**Find, delete after confirmation**
find /Users/Hans/JavaPaintProjects -type f -name "*paint*.jar" -exec rm -vi {} \;
find /Users/Hans/.m2 -type f -name "*paint*.jar" -exec rm -vi {} \;

**Find, delete**
find /Users/Hans/JavaPaintProjects -type f -name "*paint*.jar" -exec rm -v {} \;
find /Users/Hans/.m2 -type f -name "*paint*.jar" -exec rm -v {} \;


**Prevent Mac to sleep**
Open terminal window
caffeinate -di
Close terminal window to restore normal setting

**Install Java JRE 8
https://adoptium.net/en-GB/temurin/releases?version=8&os=any&arch=any

find "/Users/Hans/Paint Test Project/" -type f -name "Recordings.csv" -exec rm -v {} \;



1. Ensure your working directory is clean (no uncommitted changes).
2. Save the patch file (e.g., ProjectDialog-header.patch) into your project root.
3. Run a dry-run check:  
   git apply --check ProjectDialog-header.patch
4. Apply the patch:  
   git apply ProjectDialog-header.patch
5. Review changes in your IDE/editor and verify everything looks correct.
6. Stage and commit the changes:  
   git add paint/shared/dialogs/ProjectDialog.java  
   git commit -m "Add file header and JavaDoc to ProjectDialog"
7. Push your commit (if using a remote repository).
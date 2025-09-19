Find all the jar file with paint in the name
find / -type f -name "*paint*.jar" 2>/dev/null


Find all the jar files in the development environment with paint in the name
find /users/Hans/JavaPaintProjects -type f -name "*paint*.jar" 2>/dev/null

Find, delete after confirmation
sudo find / -type f -name "*paint*.jar" -ok rm {} \; 2>/dev/null
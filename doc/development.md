**Find all the jar file with paint in the name**
`find / -type f -name "*paint*.jar" 2>/dev/null`

**Find all the jar files in the development environment with paint in the name**
`find /users/Hans/JavaPaintProjects -type f -name "*paint*.jar"`
`find /users/Hans/.m2 -type f -name "*paint*.jar"`

**Find, delete after confirmation**
`find /Users/Hans/JavaPaintProjects -type f -name "*paint*.jar" -exec rm -v {} \;`
`find /Users/Hans/.m2 -type f -name "*paint*.jar" -exec rm -v {} \;`

**Prevent Mac to sleep**
`Open terminal window`
`caffeinate -di`
`Close terminal window to restore normal setting`


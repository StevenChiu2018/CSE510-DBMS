# Define paths and compiler settings
JDKPATH = ${SYSJDKPATH}
LIBPATH = ${SYSLIBPATH}
CLASSPATH = $(JDKPATH)/lib/classes.zip:.:..:$(LIBPATH)
BINPATH = $(JDKPATH)/bin
JAVAC = $(JDKPATH)/bin/javac -classpath $(CLASSPATH)
JAVA  = $(JDKPATH)/bin/java  -classpath $(CLASSPATH)

# Define the java source files
SOURCES = Heapfile.java HFPage.java Tuple.java Scan.java Columnarfile.java

# Define the target class files (derived from SOURCES)
CLASSES = $(SOURCES:.java=.class)

# Default target
all: $(CLASSES)

# Rule for compiling java source files
%.class: %.java
	$(JAVAC) $<

# Clean up compiled files and editor backup files
clean:
	\rm -f *.class *~ \#* core

# Additional dependencies
Columnarfile.class: Heapfile.class HFPage.class Tuple.class Scan.class

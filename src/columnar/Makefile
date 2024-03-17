JDKPATH = ${SYSJDKPATH}
LIBPATH = ${SYSLIBPATH}
CLASSPATH = $(JDKPATH)/lib/classes.zip:.:..:$(LIBPATH)
BINPATH = $(JDKPATH)/bin
JAVAC = $(JDKPATH)/bin/javac -classpath $(CLASSPATH)
JAVA  = $(JDKPATH)/bin/java  -classpath $(CLASSPATH)

PROGS = together

all: $(PROGS)

bi:BatchInsert.java
	$(JAVAC) BatchInsert.java

together:*.java
	$(JAVAC) *.java

clean:
	\rm -f *.class *~ \#* core

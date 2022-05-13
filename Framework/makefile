# Makefile for CS3 Scheculing assignment 2022

LIB = ../lib
SRCDIR = src
BINDIR = bin
TESTDIR = test
DOCDIR = doc

CLI = $(LIB)/cli/commons-cli-1.3.1.jar
ASM = $(LIB)/asm/asm-5.0.4.jar:$(LIB)/asm/asm-commons-5.0.4.jar:$(LIB)/asm/asm-tree-5.0.4.jar
JUNIT = $(LIB)/junit/junit-4.12.jar:$(LIB)/junit/hamcrest-core-1.3.jar
JACOCO = $(LIB)/jacoco/org.jacoco.core-0.7.5.201505241946.jar:$(LIB)/jacoco/org.jacoco.report-0.7.5.201505241946.jar:
TOOLS = $(LIB)/tools

JAVAC = javac
JFLAGS = -g -d $(BINDIR) -cp $(BINDIR):$(JUNIT)


vpath %.java $(SRCDIR)/simulator:$(SRCDIR)/processgenerator:$(TESTDIR)
vpath %.class $(BINDIR)/simulator:$(BINDIR)/processgenerator:$(BINDIR)

# define general build rule for java sources
.SUFFIXES:  .java  .class

.java.class:
	$(JAVAC)  $(JFLAGS)  $<

#default rule - will be invoked by make


framework: 
	javac $(JFLAGS) $(SRCDIR)/simulator/*.java
	
generator:
	javac $(JFLAGS) $(SRCDIR)/processgenerator/*.java
	
simulator: 
	javac $(JFLAGS) $(SRCDIR)/*.java

all: framework generator simulator

run:
	java -ea -cp bin Simulate


# Rules for generating documentation
doc:
	javadoc -d $(DOCDIR) $(SRCDIR)/simulator/*.java

clean:
	@rm -f  $(BINDIR)/*.class
	@rm -f $(BINDIR)/*/*.class
	@rm -Rf doc

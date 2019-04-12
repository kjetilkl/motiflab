![MotifLab](https://www.motiflab.org/images/motiflab2_header.png)

<p align="center">
    <img src ="https://img.shields.io/badge/version-2.0-blueviolet.svg"/>
    <img src ="https://img.shields.io/badge/platform-windows|linux|macos-yellow.svg"/>
    <img src ="https://img.shields.io/badge/java-1.8-blue.svg" />
</p>

MotifLab is a general workbench for analysing regulatory regions and discovering transcription factor binding sites and cis-regulatory modules in DNA sequences.
For more information, including screenshots and examples, please visit the project web site at ![www.motiflab.org](https://www.motiflab.org)

![Screenshot](https://www.motiflab.org/screenshots/motiflab_github_screenshot.png =850px)

### Note

MotifLab version 2.0 is currently under development and is still considered to be in "pre-release" stage.


### Prerequisites

MotifLab is written in Java. To build the project from source you will need:

* [Java JDK 8](https://www.java.com) - programming language
* [Maven](https://maven.apache.org/) - build and dependency manager

Most of the dependencies are available from the central Maven repository and will be handled automatically.
However, one dependency - [bigwig-1.0.jar](https://www.motiflab.org/dependencies/bigwig-1.0.jar) - must currently be dealt with manually. 
To install this dependency in your local Maven repository, download the file and run the following command:

```
mvn install:install-file -Dfile=pathTo/bigwig-1.0.jar -DgroupId=org.broad.igv -DartifactId=bigwig -Dversion=1.0 -Dpackaging=jar
```

### Building from source

To compile the Java files and package the project, go to the directory containing the "pom.xml" file and run the Maven command:

```
mvn package
```

Maven will package MotifLab in a JAR-file and place it in the "target" subdirectory. Other dependencies will be placed in "target/lib". 


## Running MotifLab

To start MotifLab, go into the "target" directory and run:

```
java -jar MotifLab-2.0.jar
```

This will start up MotifLab with the normal graphical user interface. You can choose a simpler graphical interface with the command:

```
java -jar MotifLab-2.0.jar -minimalGUI
```

MotifLab can also be run as a command-line tool without a graphical user interface. 
In this case you must supply a protocol file that describes the operations to be performed and a file defining which sequences to run the analysis on (unless these are specified in the protocol itself).

```
java -jar MotifLab-2.0.jar -cli -p <protocol_file> \[-s <sequence_file>\]
```

## Authors

* **Kjetil Klepper** (kjetil.klepper@ntnu.no)


## Additional Information

MotifLab was originally developed as a PhD-project by Kjetil Klepper under the supervision of Professor Finn Drabløs at the Norwegian University of Science and Technology (NTNU). 
A [scientific paper](https://doi.org/10.1186/1471-2105-14-9) was published in BMC Bioinformatics.

The project was created with [NetBeans IDE](https://netbeans.org/) using the Swing Application Framework for some of the GUI design. 
The SAF framework is no longer supported by NetBeans (as of version 7), but it is still possible to compile the code.



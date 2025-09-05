![MotifLab](https://www.motiflab.org/images/motiflab2_header.png)

<p align="center">
    <img src ="https://img.shields.io/badge/version-2.0-blueviolet.svg"/>
    <img src ="https://img.shields.io/badge/platform-windows|linux|macos-yellow.svg"/>
    <img src ="https://img.shields.io/badge/java-23-blue.svg" />
</p>

MotifLab is a general workbench for analysing regulatory regions and discovering transcription factor binding sites and cis-regulatory modules in DNA sequences.
For more information, including screenshots and examples, please visit the project web site at [www.motiflab.org](https://www.motiflab.org)

![Screenshot](https://www.motiflab.org/screenshots/motiflab_github_screenshot.png)


> [!WARNING]  
> MotifLab version 2.0 is currently under development and is still considered to be in "pre-release" stage.
> The final released version may not be 100% compatible with pre-release versions (especially when it comes to saved sessions).


### Prerequisites

MotifLab is written in Java. To build the project from source you will need:

* [Java JDK 8](https://www.java.com) - programming language
* [Maven](https://maven.apache.org/) - build and dependency manager

> [!IMPORTANT]  
> Note that it is currently not possible to build MotifLab with a more recent JDK than JDK 8, because MotifLab contains a class named "Module" (referring to _cis-regulatory modules_)
> that conflicts with the Module class that was introduced in Java 9.

MotifLab relies on a package published in GitHub Packages. To obtain this dependency, you must add a `<server>` block for the "github" server configured with your GitHub username and token in your Maven settings file `settings.xml`. (This file is usually found within the hidden `.m2` folder in your home directory. On Unix/Linux systems, this would be `~/.m2/settings.xml`. On windows, it could be `C:\Users\{USERNAME}\.m2\settings.xml`). Failing to add this server block will lead to a "401 Unauthorized" error.  
```xml
<settings>

  <servers>
        <server>
            <id>github</id>
            <username>{your_GitHub_username}</username>
            <password>{your_GitHub_token}</password>
        </server>
  </servers>
  
</settings>
```


### Building from source

To compile the Java files and package the project, clone the repository, go to the directory containing the "pom.xml" file and run the Maven package command:

```
git clone https://github.com/kjetilkl/motiflab.git
cd motiflab
mvn package
```

Maven will package MotifLab into a JAR-file and place it in the "target" subdirectory. The "motiflab-2.0-standalone.jar" file is a "fat" JAR that also includes all external dependencies
that are required to run MotifLab. The second JAR-file named "motiflab-2.0.jar" only contains the core MotifLab code, but the external dependencies can be found in the directory "target/lib". 


## Running MotifLab

To start MotifLab, go into the "target" directory and run:

```
java -jar MotifLab-2.0.jar
```

This will start up MotifLab with the normal graphical user interface. On some operating systems you may also be able to start MotifLab simply by double-clicking the JAR-file.

You can choose a simpler graphical interface with the command:

```
java -jar MotifLab-2.0.jar -minimalGUI
```

MotifLab can also be run as a command-line tool without a graphical user interface. 
In this case you must supply a protocol file that describes the operations to be performed and a file defining which sequences to run the analysis on (unless these are specified in the protocol itself).

```
java -jar MotifLab-2.0.jar -cli -p <protocol_file> [-s <sequence_file>]
```

## Authors

* **Kjetil Klepper** (kjetil.klepper@ntnu.no)


## Additional Information

MotifLab was originally developed as a PhD-project by Kjetil Klepper under the supervision of Professor Finn Drabløs at the Norwegian University of Science and Technology (NTNU). 
A [scientific paper](https://doi.org/10.1186/1471-2105-14-9) was published in BMC Bioinformatics.

The project was created with [NetBeans IDE](https://netbeans.org/) using the Swing Application Framework for some of the GUI design. 
The SAF framework is no longer supported by NetBeans (as of version 7), but it is still possible to compile the code.



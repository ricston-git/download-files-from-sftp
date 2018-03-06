# Download Files Using Mule's SFTP Connector

## Introduction

This project is a concrete example of what is explained on [Ricston's blog](https://www.ricston.com/blog/polling-multiple-nodes-mule-esb-cluster/) about downloading files from an SFTP server in the middle of a Mule flow.

This repository contains a Maven-based Mule project. Its aim is to demonstrate how to download files in the middle of a flow using a Java component, which in turn uses Mule's SFTP transport only. The benefit of this method is that it allows you to adapt a single class depending on any project's requirements. The reason why this method of retrieving files was devised was because Mule's endpoint model is quite restrictive (inbound to read, outbound to write), and the Mule module requester does not respect the order in which files are read - it simply runs an `ls` SFTP command and returns the first file in the list.

## Getting Started

In order to run this project, you need:

- Mule 3.8.x or Mule 3.9.x installed in Anypoint Studio.
- Apache Maven
- An SFTP server

An SFTP server has been provided along with the project in the form of a Docker container which is described in a Docker compose file under `src/main/resources`. If you have docker installed, simply run `mvn docker:start`. This will start an SFTP server on localhost:22. The username and password can be configured by changing the docker compose file. If you want to stop the docker container, simply run `mvn docker:stop`. The docker image being used is [atmoz/sftp](https://github.com/atmoz/sftp), and the Docker Maven plugin being used is the one developed by [fabric8.io](https://github.com/fabric8io/docker-maven-plugin).

`src/main/resources/sftp` is mounted as the root folder of the SFTP server.

## Running the Project

To run this project, clone this repository and import it into your workspace. Once Anypoint Studio has finished downloading any Maven dependencies which you're missing, simply click the Run button. This will start your project and Mule will start polling the SFTP server for new files.

This project uses two types files:

- INSTRUCT files, which instruct Mule to download other files from the same SFTP folder. 
- Generic files which contain arbitrary data.

There is one INSTRUCT file packaged with this project, called *Sample.instruct*:
```
SampleFile2.txt
SampleFile1.txt
SampleFile3.txt
```

The INSTRUCT file above will instruct Mule to first download SampleFile2.txt, then SampleFile1.txt and finally SampleFile3.txt. The order in which files are downloaded may or may not be required depending on your usecase.

The sample files contain arbitrary strings which denote the file which is currently being processed. In the real world, these can be CSV files, Microsoft Excel files, files with bespoke file formats, and so on. _The intention of this project is not to shown how to parse these files, but rather, how to download them mid-flow._

When you run the project, simply move the sample files to the SFTP server (or `src/main/resources/sftp/blogdata/` if you're using the packaged Docker image) and then do the same for the INSTRUCT file. Mule is configured to poll for new INSTRUCT files every 10 seconds by default. Eventually, Mule will pick up the sample files and "process" them in the order specified within the INSTRUCT file. Any files read by Mule are automatically moved to a folder called 'archived', so if you want to reprocess the files, just refresh your Studio project and move the files again.
#!/bin/bash
echo "####### Removing class files and lib jars #############"
rm -rf class/*.class
rm -rf lib/*
echo "####### Downloading dependent jars #############"
cd lib
wget http://central.maven.org/maven2/org/apache/yetus/audience-annotations/0.5.0/audience-annotations-0.5.0.jar
wget http://central.maven.org/maven2/jline/jline/0.9.94/jline-0.9.94.jar
wget https://archive.apache.org/dist/logging/log4j/1.2.17/log4j-1.2.17.jar
wget http://central.maven.org/maven2/io/netty/netty/3.10.6.Final/netty-3.10.6.Final.jar
wget http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar
wget http://central.maven.org/maven2/org/slf4j/slf4j-log4j12/1.7.25/slf4j-log4j12-1.7.25.jar
wget http://central.maven.org/maven2/org/apache/zookeeper/zookeeper/3.4.12/zookeeper-3.4.12.jar
cd ../
echo "####### Installing java #############"
sudo add-apt-repository -y ppa:webupd8team/java
sudo apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
sudo apt-get install -y oracle-java8-installer
echo "####### Compiling Source Code ###########"
javac -cp .:./lib/* src/*.java -d class/
echo "####### Successful ###########"
echo ""
echo "Please execute the following scripts to test on each terminal"
echo "cd class"
echo "source ./config.sh"
echo "The above script will set alias"
echo "Start testing"
echo "watcher 12.34.45.87:6000 N"
echo "player 12.34.45.87:6000 name"
echo "player 12.34.45.87:6000 \"first last\""
echo "player 12.34.45.87:6000 name count delay score"


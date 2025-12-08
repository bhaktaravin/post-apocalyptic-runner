#!/usr/bin/env fish
# Setup script to configure Java 21 LTS for this project

set -gx JAVA_HOME /home/bhaktaravin/.jdk/jdk-21.0.8
set -gx PATH $JAVA_HOME/bin $PATH

echo "Java environment configured:"
java -version

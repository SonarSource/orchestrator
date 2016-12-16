[![Build Status](https://travis-ci.org/SonarSource/orchestrator.svg)](https://travis-ci.org/SonarSource/orchestrator)

# How to build
`mvn clean install`

# Prerequisites
* Make sure you have your proper orchestrator.properties file, and MAVEN_HOME variable set
```
export MAVEN_OPTS="-XX:MaxPermSize=128M -server"
export MAVEN_LOCAL_REPOSITORY=/path/to/local/repo
export MAVEN_REMOTE_REPOSITORY=http://nexus.internal.sonarsource.com/nexus/content/groups/ss-repo
```
* If running with Oracle as the database, must have jdbc ojdbc14 drive in your maven repository
* make sure you have firefox available through the command line

# For running orchestrator with a custom Maven installation, add Java properties :
```
-Dmaven.localRepository="C:/your/maven/repository"
-Dmaven.home="C:/your/mavenX"
-Dmaven.binary=mvnDebug   (binary located in [maven.home]/bin/, without .bat or .cmd extension on Windows)
```

### License

Copyright 2008-2016 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
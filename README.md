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

# For running orchestrator behind a corporate proxy, add Java properties :
```
-Dhttp.proxyHost=proxy.company.com
-Dhttp.proxyPort=80
-Dhttps.proxyHost=proxy.company.com
-Dhttps.proxyPort=80
-Dhttp.nonProxyHosts="localhost|127.0.0.1|*.company.com"
-Dhttp.proxyUser=foo
-Dhttp.proxyPassword=bar
```

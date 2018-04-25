[![Build Status](https://travis-ci.org/SonarSource/orchestrator.svg?branch=master)](https://travis-ci.org/SonarSource/orchestrator)  [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.orchestrator%3Aorchestrator-parent&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.orchestrator%3Aorchestrator-parent)

Orchestrator is a Java library to install SonarQube from tests.

## API

An instance of class `com.sonar.orchestrator.Orchestrator` represents a SonarQube server:
 
    Orchestrator orchestrator = Orchestrator.newBuilder()
      .setSonarVersion("7.0")
      .addPlugin(FileLocation.of("/path/to/plugin.jar"))
      .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "5.2.0.13398")
      .setServerProperty("sonar.web.javaOpts", "-Xmx1G")
      .build();
     
    // start server
    orchestrator.start();
    
    // run SonarQube Scanner
    orchestrator.executeBuild(SonarScanner.create(new File("/path/to/project")));
    
    // requests web services
    String baseUrl = orchestrator.getServer().getUrl();
    // ...
    
    orchestrator.stop();
    
## Version Aliases

Aliases can be used to define the versions of SonarQube and plugins to be installed. Supported values are:

* `LATEST_RELEASE` for the latest release (in terms of version number, not date)
* `LATEST_RELEASE[x.y]` for the latest release of a series, for example `LATEST_RELEASE[5.2]`
* `DEV` for the latest official build (in terms of version number, not date)
* `DEV[x.y]` for the latest official build of a series. For example `DEV[5.2]` may install version `5.2.0.1234`.

The alias `LTS` is no more supported for SonarQube since Orchestrator 3.17. It should be replaced by `LATEST_RELEASE[6.7]`.
## Local Cache

The artifacts downloaded from Artifactory (SonarQube, plugins) are copied to the local directory `~/.sonar/orchestrator/cache`.
This directory is *not* automatically purged and may grow significantly when using the version alias
`DEV`.

## Configuration

Test environment is defined in the file `~/.sonar/orchestrator/orchestrator.properties`:

    # Token used to download SonarSource private artifacts from https://repox.sonarsource.com
    # Generate your API key at https://repox.sonarsource.com/webapp/#/profile
    #orchestrator.artifactory.apiKey=xxx
    
    # Personal access token used to request SonarSource development licenses at https://github.com/sonarsource/licenses. 
    # Generate a token from https://github.com/settings/tokens
    #github.token=xxx
      
    # Port of SonarQube server. Default value is 0 (random).
    #orchestrator.container.port=10000
    
    # Maven installation, used when running Scanner for Maven.
    # By default Maven binary is searched in $PATH
    #maven.home=/usr/local/Cellar/maven/3.5.0/libexec
    
    # Maven local repository (optional), used to search artifacts of plugins before
    # downloading from Artifactory. 
    # Default is ~/.m2/repository
    #maven.localRepository=/path/to/maven/repository
    
    # Instance of Artifactory. Default is SonarSource's instance.
    #orchestrator.artifactory.url=https://repox.sonarsource.com

The path to configuration file can be overridden with the system property `orchestrator.configUrl` 
or the environment variable `ORCHESTRATOR_CONFIG_URL`.
Example: `-Dorchestrator.configUrl=file:///path/to/orchestrator.properties`


## HTTP Proxy

For running Orchestrator behind a corporate proxy, add Java properties :

    -Dhttp.proxyHost=proxy.company.com
    -Dhttp.proxyPort=80
    -Dhttps.proxyHost=proxy.company.com
    -Dhttps.proxyPort=80
    -Dhttp.nonProxyHosts="localhost|127.0.0.1|*.company.com"
    -Dhttp.proxyUser=foo
    -Dhttp.proxyPassword=bar

## License

Copyright 2011-2018 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)

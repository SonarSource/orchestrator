[![Build Status](https://github.com/SonarSource/orchestrator/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/SonarSource/orchestrator/actions/workflows/build.yml?query=branch%3Amaster)
Orchestrator is a Java library to install and run SonarQube from tests.

## JUnit 4 API

An instance of class `com.sonar.orchestrator.junit4.OrchestratorRule` can be used as `Rule` or `ClassRule`. It will represent a SonarQube server that will be started before tests, and stopped after:

```
public class MyTest {

    @ClassRule
    public static OrchestratorRule ORCHESTRATOR = OrchestratorRule.builderEnv()
      .setSonarVersion("7.0")
      .addPlugin(FileLocation.of("/path/to/plugin.jar"))
      .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "5.2.0.13398")
      .setServerProperty("sonar.web.javaOpts", "-Xmx1G")
      .build();

    @Test
    public void myTest() {
        // run SonarQube Scanner
        ORCHESTRATOR.executeBuild(SonarScanner.create(new File("/path/to/project")));

        // requests web services
        String baseUrl = ORCHESTRATOR.getServer().getUrl();
        // ...
    }

}
```

## JUnit 5 API

An instance of class `com.sonar.orchestrator.junit5.OrchestratorExtension` can be used as `Extension`. It will represent a SonarQube server that will be started before tests, and stopped after:

```
class MyTests {

    @RegisterExtension
    static OrchestratorExtension ORCHESTRATOR = OrchestratorExtension.builderEnv()
      .setSonarVersion("7.0")
      .addPlugin(FileLocation.of("/path/to/plugin.jar"))
      .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "5.2.0.13398")
      .setServerProperty("sonar.web.javaOpts", "-Xmx1G")
      .build();

    @Test
    void myTest() {
        // run SonarQube Scanner
        ORCHESTRATOR.executeBuild(SonarScanner.create(new File("/path/to/project")));

        // requests web services
        String baseUrl = ORCHESTRATOR.getServer().getUrl();
        // ...
    }

}
```

## Version Aliases

Aliases can be used to define the versions of SonarQube and plugins to be installed. Supported values are:

- `DEV` for the latest official build (in terms of version number, not date)
- `DEV[x.y]` for the latest official build of a series. For example `DEV[5.2]` may install version `5.2.0.1234`.
- `LATEST_RELEASE` for the latest release (in terms of version number, not date)
- `LATEST_RELEASE[x.y]` for the latest release of a series, for example `LATEST_RELEASE[5.2]`

The alias `LTS` is no more supported for SonarQube since Orchestrator 3.17. It should be replaced by `LATEST_RELEASE[6.7]`.

Please note that since Orchestrator 4.7, if the default value of `orchestrator.artifactory.url` (https://repox.jfrog.io/repox) is _not_ used, 
the `DEV` alias will not work.

## Local Cache

The artifacts downloaded from Artifactory (SonarQube, plugins) are copied to the local directory `~/.sonar/orchestrator/cache`.
This directory is _not_ automatically purged and may grow significantly when using the version alias
`DEV`.

## Configuration

The test environment is configured in the file `~/.sonar/orchestrator/orchestrator.properties`:

    # Token used to download SonarSource private artifacts from https://repox.jfrog.io/repox
    # Generate your identity token at https://repox.jfrog.io/ui/user_profile
    # This property can be replaced by the environment variable ARTIFACTORY_ACCESS_TOKEN.
    #orchestrator.artifactory.accessToken=xxx

    # Fine-grained Personal access token used to request SonarSource development licenses at https://github.com/sonarsource/licenses.
    # Generate a token from https://github.com/settings/personal-access-tokens/new
    # The token needs `SonarSource` as the Resource Owner and Read-only access for the SonarSource/licenses repository.
    # This property can be replaced by the environment variable GITHUB_TOKEN.
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

    # Instance of Artifactory. Default is SonarSource's instance (https://repox.jfrog.io/repox).
    # This property can be replaced by the environment variable ARTIFACTORY_URL.
    # To use maven central instead, use https://repo1.maven.org/maven2
    # To use a custom instance, use your own URL that points to a Maven repository.
    #orchestrator.artifactory.url=https://repo1.maven.org/maven2

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

Copyright 2011-2024 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)

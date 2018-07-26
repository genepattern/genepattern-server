### Building the awsbatch jobrunner jar file

#### To create create a new release
```
    mvn package
```
This creates a number of single-file distributions, via the maven assembly plugin. For example,
  gp-awsbatch-0.2.1-snapshot.6-bin.zip
  gp-awsbatch-0.2.1-snapshot.6-bin.tar.gz
  
#### To view the junit report
```
    mvn surefire-report:report
    mvn site -DgenerateReports=false
    open target/site/surefire-report.html
```

### To deploy the latest build snapshot to the server
for example, v0.2.1, build 6
```
    mvn clean package
    # sanity check
    unzip -l target/gp-awsbatch-0.2.1-snapshot.2-bin.zip
    # copy to server
    scp target/gp-awsbatch-0.2.1-snapshot.2-bin.zip gp-beta-ami:/opt/gpbeta/installer
    
    #
    ssh gp-beta-ami
    cd /opt/gpbeta
    # sanity check
    unzip -l /opt/gpbeta/installer/gp-awsbatch-0.2.1-snapshot.2-bin.zip -d /opt/gpbeta
    # stop the server
    cd /opt/gpbeta/gp
    ./stop-genepattern.sh
    # (back on the dev machine, backup the config file)
      ant -p
      ant download-files
      download-aws-batch-scripts
    
    # back on the gpbeta server, deploy the new artifacts ...
    unzip /opt/gpbeta/installer/gp-awsbatch-0.2.1-snapshot.2-bin.zip -d /opt/gpbeta
    # make sure to delete the previous version of the library, e.g.
    cd /opt/gpbeta
    ls gp/Tomcat/webapps/gp/WEB-INF/lib/gp-awsbatch-0.*
    rm gp/Tomcat/webapps/gp/WEB-INF/lib/gp-awsbatch-0.1.10-snapshot.1.jar
    
    # double check config files, then ...
    # star the server
    cd /opt/gpbeta/gp
    ./start-genepattern.sh
```

The old way:
```
    scp target/gp-awsbatch-0.1.4-snapshot.5.jar gp-beta-ami:/opt/gpbeta/installer
    scp -rpv src/main/scripts/* gp-beta-ami:/opt/gpbeta/installer/wrapper_scripts/aws_batch
    scp -rpv src/main/scripts/* gp-beta-ami:/opt/gpbeta/gp_home/resources/wrapper_scripts/aws_batch
```

Note: the latest version is deployed here:
  /opt/gpbeta/gp/Tomcat/webapps/gp/WEB-INF/lib/gp-awsbatch-0.2.1-snapshot.2.jar

### Maven notes

To only download dependencies without doing anything else
```
 mvn dependency:resolve
```

To download a single dependency
```
  mvn dependency:get -Dartifact=groupId:artifactId:version 
```

To display a list of dependencies with newer versions available
```
  mvn versions:display-dependency-updates
```

To display a list of those plugins with newer versions available
```
  mvn versions:display-plugin-updates
```

Describe the assembly plugin
```
mvn help:describe -Dplugin=assembly [-Ddetail=true]
```

Display the effective pom
```
mvn help:effective-pom
```

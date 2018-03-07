### Building the awsbatch jobrunner jar file

#### To create create a new release
```
    mvn package
```

### To deploy the latest build snapshot to the server
for example, buildNumber=5
```
    scp target/gp-awsbatch-0.1.4-snapshot.5.jar gp-beta-ami:/opt/gpbeta/installer
    scp -rpv src/main/scripts/* gp-beta-ami:/opt/gpbeta/installer/wrapper_scripts/aws_batch
    scp -rpv src/main/scripts/* gp-beta-ami:/opt/gpbeta/gp_home/resources/wrapper_scripts/aws_batch
```

Note: the latest version is deployed here:
  /opt/gpbeta/gp/Tomcat/webapps/gp/WEB-INF/lib/gp-awsbatch-0.1.7-snapshot.1.jar

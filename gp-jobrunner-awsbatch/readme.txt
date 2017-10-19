#
# Notes/Example commands for pushing updates to the gp-dev-ami server
#

To create create a new release
  mvn package

To deploy the latest build snapshot to the server
for example, buildNumber=5
  scp target/gp-awsbatch-0.1.4-snapshot.5.jar gp-dev-ami:/opt/genepattern/installer
  scp -rpv src/main/scripts/* gp-dev-ami:/opt/genepattern/gp_home/resources/wrapper_scripts/aws_batch
  scp -rpv src/main/scripts/* gp-dev-ami:/opt/genepattern/installer/wrapper_scripts/aws_batch

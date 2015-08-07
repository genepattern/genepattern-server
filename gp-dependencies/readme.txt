Created this pom file to assist with manually updating jar files in the GP WEB-INF/lib folder.

    https://maven.apache.org/guides/getting-started/index.html#How_do_I_make_my_first_Maven_project
    https://maven.apache.org/configure.html


Example commands:

# created the project
mvn -B archetype:generate   -DarchetypeGroupId=org.apache.maven.archetypes   -DgroupId=org.genepattern.server -DartifactId=gp-dependencies

# edited the POM file by browsing http://mvnrepository.com

# copied dependencies to this folder
mvn dependency:copy-dependencies

# download source (useful for debugging); make sure to configure a non-hidden M2_REPO directory
# so that it's easier to select source code from Eclipse or other IDE
mvn dependency:sources

# didn't try this, but it looks useful
mvn eclipse:eclipse


Note: BroadCore requires Java 6

To integrate this library into your GenePattern Server you need to:
1. Configure your GP server
2. Install the BroadCore library into your web app.
3. Install the GP LSF library into your web app. 

1. Configuring the GenePattern Server
    Note: This has been tested with the default Tomcat 5.5 server which comes with GenePattern
    See: http://tomcat.apache.org/tomcat-5.5-doc/jndi-datasource-examples-howto.html
    
1.a) Review the settings in the resources/genepattern.propertes file
    hibernate.configuration.file=hibernate.cfg.xml
    database.vendor=ORACLE
    command.manager.parser=org.genepattern.server.executor.BasicCommandManagerParser
    command.manager.config.file=job_configuration.yaml
    
1.b) Review the settings in Tomcat/webapps/gp/META-INF/context.xml

1.c) Review the settings in Tomcat/webapps/gp/WEB-INF/classes/hibernate.cfg.xml
    You need to reference the JNDI datasource defined in the context.xml file.
    You also need to enter the correct db username, password, and default schema.

1.d) Edit the resources/job_configuration.yaml file 
    ...
    LSF:
        classname: org.genepattern.server.executor.lsf.LsfCommandExecutor
        #BroadCore configuration properties
        configuration.properties:
            hibernate.connection.datasource: java:comp/env/jdbc/gp/oracle
            hibernate.default_schema: GENEPATTERN_DEV_01
            hibernate.dialect: org.genepattern.server.database.PlatformOracle9Dialect
            hibernate.current_session_context_class: thread
            hibernate.transaction.factory_class: org.hibernate.transaction.JDBCTransactionFactory
            # number of seconds to check for completed jobs
            lsf.check.frequency: 5
            lsf.num.job.submission.threads: 3
            lsf.num.job.completion.threads: 3    

1.e) Double check that your JDBC driver is on the classpath. You can put it in the Tomcat/webapps/gp/WEB-INF/lib or Tomcat/common/lib folder. 
    Drivers for oracle (ojdbc14.jar) and HSQLDB (hsqldb.jar) are already included by the GenePattern installer.

2. Checkout, build, and deploy the Broad Core library
2.a) e.g. mkdir ~/Projects/BroadCore; cd Projects
    svn co https://svn.broadinstitute.org/BroadCore/trunk BroadCore
    Follow the build instructions.

2.b) Copy broad core and dependent libraries to the gp web app
    cd <GenePatternServer>
    cp ~/Projects/BroadCore/dist/broad-core-main-2.9.2.jar Tomcat/webapps/gp/WEB-INF/lib
    cp -i ~/Projects/BroadCore/lib/deploy/*.jar Tomcat/webapps/gp/WEB-INF/lib
    cp -i ~/Projects/BroadCore/lib/deploy/hibernate/*.jar Tomcat/webapps/gp/WEB-INF/lib
    
    Note: double-check the ojdbc14.jar in the broad core is the same as that in Tomcat/common/lib. I don't know what happens if they differ.
 
3. Build and deploy the gp-lsf-*.jar file. There are instructions in the build.xml file. 
You need to configure that path to the broad core library and the GP server libraries.
After you build, deploy.
    cp dist/*.jar Tomcat/webapps/gp/WEB-INF/lib 

    Run one of the lsf_schema_*.sql scripts to create teh gp_lsf tables.
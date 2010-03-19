To integrate this library into your GenePattern Server you need to:
1. Configure Tomcat to provide a JNDI DataSource to the GP database
2. Install the BroadCore library into your web app.
3. Install the GP LSF library into your web app. 

1. Configuring the JNDI DataSource
    Note: These instructions are specific to Tomcat 5.5
    
1.a) Add an entry in the Tomcat/conf/server.xml file. Use the correct password.
  <GlobalNamingResources>
    ...
    <!-- DataSource to the GP Oracle DB  -->
    <Resource 
        name="jdbc/db1"
        auth="Container"
        type="oracle.jdbc.pool.OracleDataSource"
        driverClassName="oracle.jdbc.OracleDriver"
        factory="oracle.jdbc.pool.OracleDataSourceFactory"
        url="jdbc:oracle:thin:@cmapdb01.broadinstitute.org:1521:cmap_dev"
        user="GENEPATTERN_DEV_01"
        password="****"
        maxActive="20"
        maxIdle="10"
        maxWait="-1" />
    ...
  </GlobalNamingResources>

1.b) Add an entry in the Tomcat/conf/context.xml file.
  <Context>
    ...
    <ResourceLink global="jdbc/db1" name="jdbc/db1" type="oracle.jdbc.pool.OracleDataSource"/>
    ...
  </Context>
  
1.c) Instead of step (1a) you could configure the datasource within the web application. Edit Tomcat/webapps/gp/WEB-INF/web.xml. Add the following lines right after '<display-name>GenePattern</display-name>'.
  <!-- added JNDI datasource to oracle DB -->
  <resource-ref>
    <description>Reference to Oracle GP Database</description>
    <res-ref-name>jdbc/db1</res-ref-name>
    <res-type>oracle.jdbc.pool.OracleDataSource</res-type>
    <res-auth>Container</res-auth>
  </resource-ref>

1.d) finally, move your JDBC driver to the Tomcat/common/lib folder. For oracle it is ojdbc14.jar.
    mv webapps/gp/WEB-INF/lib/ojdbc14.jar common/lib

    
1.e) replace the Tomcat/webapps/gp/WEB-INF/classes/hibernate.cfg.xml with the following
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-configuration PUBLIC "-//Hibernate/Hibernate Configuration DTD 3.0//EN" "http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd">

<hibernate-configuration> 
  <session-factory> 
    <!-- use a JNDI datasource provided by the Tomcat 5.5 container -->
    <property name="show_sql">false</property> 
    <property name="hibernate.connection.datasource">java:comp/env/jdbc/db1</property>
    <property name="hibernate.default_schema">GENEPATTERN_DEV_01</property> <!-- does not work for sql named queries -->
    <property name="hibernate.current_session_context_class">thread</property>
    <property name="hibernate.transaction.factory_class">org.hibernate.transaction.JDBCTransactionFactory</property>
    <property name="hibernate.dialect">org.genepattern.server.database.PlatformOracle9Dialect</property> 

    <!-- Mappings -->
    <mapping resource="org/genepattern/server/domain/AnalysisJob.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/JobStatus.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/Lsid.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/Props.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/Sequence.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/Suite.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/TaskAccess.hbm.xml"/>
    <mapping resource="org/genepattern/server/domain/TaskMaster.hbm.xml"/>
    <mapping resource="org/genepattern/server/message/SystemMessage.hbm.xml"/>
    <mapping resource="org/genepattern/server/user/JobCompletionEvent.hbm.xml"/>
    <mapping resource="org/genepattern/server/user/User.hbm.xml"/>
    <mapping resource="org/genepattern/server/user/UserProp.hbm.xml"/>
    <mapping resource="org/genepattern/server/auth/JobGroup.hbm.xml"/>
    
 </session-factory>
</hibernate-configuration>

2. Install the Broad Core library and required libraries. This includes updated hibernate libraries.
    cp BroadCore/dist/broad-core-main-2.9.2.jar Tomcat/webapps/gp/WEB-INF/lib
    cp -i BroadCore/lib/deploy/*.jar Tomcat/webapps/gp/WEB-INF/lib
    cp -i ~pcarr/projects/BroadCore/lib/deploy/hibernate/*.jar Tomcat/webapps/gp/WEB-INF/lib
    
    TODO: narrow down the list, but here is what I have now
    commons-beanutils-1.7.jar
    commons-collections-2.1.1.jar
    commons-el.jar
    commons-logging.jar
    cos.jar
    ejb3-persistence.jar
    groovy-all-1.0.jar
    hibernate3.jar
    hibernate-annotations.jar
    poi-2.5.1-final-20040804.jar
    stripes.jar
    xstream-1.2.1.jar

    * ojdbc14.jar, the version in broad core differs from that with the gp installer. I don't know if we need to update. 
    This would go in the Tomcat/common/lib directory.

3. Install the GP LSF library
    cp dist/*.jar Tomcat/webapps/gp/WEB-INF/lib 
    
4. Edit the resources/queue.properties file.
# part 1, map queue.id to implementing class which implements org.genepattern.server.queue.CommandExecutor                                                     
#                                                                                                                                                              
queue.01.RuntimeExec=org.genepattern.server.queue.RuntimeCommandExecutor
queue.02.LSF=org.genepattern.server.queue.lsf.LsfCommandExecutor
queue.03.Test=org.genepattern.server.queue.TestCommandExecutor


#                                                                                                                                                              
# part 2, update system properties, using the naming convention, 'queue.prop.<propname>=<value>'                                                               
#                                                                                                                                                              
queue.prop.LsfCommandExecSvc.queue=genepattern
queue.prop.LsfCommandExecSvc.project=gp_3_2_3_dev
queue.prop.LsfCommandExecSvc.max.memory=2

#                                                                                                                                                              
# part 3, define the default queue, must match one of the queues defined in part 1                                                                             
#                                                                                                                                                              
default=queue.01.RuntimeExec

#                                                                                                                                                              
# part 4, map taskName to queue.id to override the default queue                                                                                               
#                                                                                                                                                              
ConvertLineEndings=queue.02.LSF
testEchoSleeper=queue.02.LSF
PreprocessDataset=queue.03.Test


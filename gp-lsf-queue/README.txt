How to integrate this library into your GenePattern Server

These instructions are specific to Tomcat 5.5

1. edit Tomcat/webapps/gp/WEB-INF/web.xml. Add the following lines right after '<display-name>GenePattern</display-name>'.
 <!-- added JNDI datasource to oracle DB -->
 <resource-ref>
  <description>
    Resource reference to a factory for java.sql.Connection
    instances that may be used for talking to a particular
    database that is configured in the server.xml file.
  </description>
  <res-ref-name>
    jdbc/myoracle
  </res-ref-name>
  <res-type>
    oracle.jdbc.pool.OracleDataSource
  </res-type>
  <res-auth>
    Container
  </res-auth>
</resource-ref>

2. replace hibernate.cfg.xml with the following
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

3. copy a bunch of jar files from the BroadCore library
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
ojdbc14.jar
poi-2.5.1-final-20040804.jar
stripes.jar
xstream-1.2.1.jar

4. Configure the JNDI datasource in Tomcat. Here is one way to do it. There are others.

a) Add an entry in the conf/web.xml file. Use the correct password.

  <GlobalNamingResources>

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

b) Add an entry in the conf/context.xml file.

  <Context>
    ...
    <ResourceLink global="jdbc/db1" name="jdbc/db1" type="oracle.jdbc.pool.OracleDataSource"/>
    ...
  </Context>

5. finally, move your JDBC drivers to the Tomcat/common/lib folder. I am using ojdbc.jar.

===============================================
How to update the GenePattern database schema
===============================================
This folder contains a collection of ordered lists of the form:
    analysis_<database.vendor>-<schemaVersion>.sql
    
When the GenePattern Server starts up, it will automatically all new DDL scripts
for the configured <database.vendor>. Scripts are run in case-insensitive alphabetical order.

The list of scripts to run is based on two things:
1) the 'database.vendor', e.g. 'analysis_mysql'; 
    Note: database.vendor=HSQLDB is shorthand for 'hypersonic'.
2) the schemaVersion of the most recent DDL script which has been saved to the database;
    Hint: "select * from props where key='schemaVersion'"

Adding a new table, modifying the schema
-----------------------------------------
Pick a new schemaVersion which must be alphabetically greater than any existing
schema versions. For example,
    
    "3.9.3" < "3.9.4"
    "3.9.3" < "3.9.3-a"

In GP <= 3.9.3, the schemaVersion and the GenePatternVersion were identical. This was
problematic for developing new interim releases of GP. 

Starting with 3.9.4, the db schemaVersion and the GenePatternVersion are no longer
required to be identical.

Create and edit a new analysis_<database.vendor>-<schemaVersion>.sql file. Add the
DDL scripts. At the end of each new file put a line like this. Make sure to replace
the <schemaVersion> with your actual schemaVersion.

-- update schema version 
update props set value='<schemaVersion>' where key='schemaVersion';
commit;


Before completing your work, it is recommended that you add a similar
DDL scripts for each of the supported database vendors.
You should also verify that the script is correct.
This is done in an ad hoc manner; usually by initializing a new 
GP instance which each of the vendors.
    
Adding a new database vendor
----------------------------
This is a rather involved process which requires manually
translating the set of DDL scripts for one of the existing database vendors so that they
are compatible with your database vendor. You may have to make some minor changes to the 
GenePattern source code to ensure compatibility with Hibernate. We have had to make
changes for each new DB vendor.

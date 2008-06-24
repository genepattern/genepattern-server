# Python script that uses the NCIBI Database interface to connect to the MiMIR2 database
#
# Note from Beth Kirschner (bkirschn@umich.edu):
#    The MiMIR2 database is publicly available using the web service interface we discussed yesterday. 
#    I've uploaded a sample perl script that executes a query and returns a list of interactions 
#    for a given gene, as we discussed.
#
#         https://portal.ncibi.org/access/content/group/64275cb8-6832-4c96-004e-cfa79eba93e4/mimir2-test.pl 
#
#
# Based on sample Java application, DbDemoClient.java, posted on the NCIBI Wiki.
#     Connect to NCIBI portal, login (e.g. as user pcarr@broad.mit.edu).
#     Go to 'NCIBI Dev' Worksite -> 'Wiki' -> 'Client-Side Web Services' -> 'Client-Side Database Web Services'
#     https://portal.ncibi.org/portal/site/NCIBI%20Sr%20Dev/page/98b4f8ef-2960-4acf-8004-c156234a815e
#
import sys
import SOAPpy
from SOAPpy import SOAPProxy

server = 'https://portal.ncibi.org/sakai-axis/NcibiDatabase.jws?wsdl'
dbServer = 'sqlserver.public'
database = 'mimiR2'

testQuery = 'select top 5 * from Molecule'
testQuery2 = 'select top 10 * from geneR2.dbo.Gene'

query = "SELECT distinct g1.symbol as gene1, g1.geneId as id1, direction, \
    it.interactionType, g2.symbol as gene2, g2.geneId as id2, g2.taxId \
FROM geneR2.dbo.Gene g1 \
    join mimiR2.dbo.MoleculeGene mg1 on g1.geneId=mg1.geneId \
    join ( \
      select intID, molID1, molID2, intTypeID, '+' as direction \
        from Interaction \
    union \
      select intID, molID2 as molID1, molID1 as molID2, intTypeID, '-' as \
        direction from Interaction \
    ) i1 on i1.molID1 = mg1.molID \
    join mimiR2.dbo.MoleculeGene mg2 on i1.molId2=mg2.molId \
    join geneR2.dbo.Gene g2 on mg2.geneId=g2.geneId \
         and g1.geneId != g2.geneId \
         and g1.taxid = g2.taxid \
    join mimiR2.dbo.InteractionType it on i1.IntTypeId=it.IntTYpeId \
where g1.symbol='CSF1R' \
order by g2.symbol"

mimi = SOAPProxy(server)
connId = mimi.anonConnection(dbServer, database)
connId = int(connId)

if connId == -1 :
    sys.stderr.write('Error: connection failed\n')
    sys.exit(-1)

ok = mimi.makeQuery(connId, query);
if ok == 0 :
    errString = mimi.getErrorString(connId);
    sys.stderr.write('Error: with query: ' + errString + '\n') 
    sys.exit(-1)

# output the results in tab delimited format, with the column names in the first row
colNames = mimi.getColNames(connId)
#print out the column names, separated by a TAB character
sys.stdout.write('\t'.join(colNames)+'\n');

while True:
    rowData = mimi.getNextManyRows(connId, 1000)
    if (rowData is None): break
    for row in rowData :
        sys.stdout.write('\t'.join(row)+'\n')

#TODO: put this in a finally clause
mimi.closeConnection(connId)


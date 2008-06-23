#!/usr/bin/perl -w
#
# Perl application that demonstrates NCIBI Database interface
#

use strict;
use SOAP::Lite;
# load SSLeay perl module from the GP module directory
use FindBin qw($Bin);
use lib "$Bin/lib/perl5/site_perl";

my $serverUrl = "https://portal.ncibi.org/sakai-axis/NcibiDatabase.jws?wsdl";

my $query = "SELECT distinct g1.symbol as gene1, g1.geneId as id1, direction,
    it.interactionType, g2.symbol as gene2, g2.geneId as id2, g2.taxId
FROM geneR2.dbo.Gene g1
    join mimiR2.dbo.MoleculeGene mg1 on g1.geneId=mg1.geneId
    join (
      select intID, molID1, molID2, intTypeID, '+' as direction
        from Interaction
    union
      select intID, molID2 as molID1, molID1 as molID2, intTypeID, '-' as
        direction from Interaction
    ) i1 on i1.molID1 = mg1.molID
    join mimiR2.dbo.MoleculeGene mg2 on i1.molId2=mg2.molId
    join geneR2.dbo.Gene g2 on mg2.geneId=g2.geneId
         and g1.geneId != g2.geneId
         and g1.taxid = g2.taxid
    join mimiR2.dbo.InteractionType it on i1.IntTypeId=it.IntTYpeId
where g1.symbol='CSF1R'
order by g2.symbol";

main:
{
   my $ok;   
   my $service = SOAP::Lite->service( $serverUrl )->on_fault (
       sub {
           my ( $soap, $res ) = @_;
           die ref $res ? $res->faultstring : $soap->transport->status, "\n";
       }
   );

   # Connect to SqlServer 
   my $connId = $service->anonConnection('sqlserver.public',"mimiR2");
   
   if ( $connId == -1 )
   {
       print "Error: connection failed\n";
       exit;
   }

   # Execute Query
   my $time = time;
   $ok = $service->makeQuery( $connId, $query );
   
   if ( !$ok )
   {
       my $errString = $service->getErrorString( $connId );
       print ("Error with query: $errString \n");
       $service->closeConnection( $connId );
       exit;
   }
   
   if ( $ok )
   {
       # Fetch and print query column names
       my @cols = $service->getColNames($connId);
       foreach (@cols)
       {
           my $aCol;
           foreach $aCol (@$_) 
           {
               print "$aCol\t";
           }
           print "\n";
       }
   
       my $eod = 0;
       while ( ! $eod )
       {
           my ($aRowSet, $aRow, $aCol);
           my @data =  $service->getNextManyRows( $connId, 1000 );
           
           foreach $aRowSet (@data) 
           {
               $eod++ if ( ! $aRow );
               foreach $aRow (@$aRowSet)
               {
                   foreach $aCol (@$aRow)
                   {
                       print "$aCol \t" if ($aCol);
                   }
                   print "\n";
               }
           }
       } 
   }
   else
   {
       print "$! \n";
   }
       
   # Close connection   
   $service->closeConnection( $connId );
}

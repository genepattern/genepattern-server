#!/usr/bin/perl -w

####################################################
# $Id$
#
# Test script for Blast submit job
#
#####################################################

use strict;
use warnings;

use lib "$ENV{OMNIGENE_HOME}/languages/perl/src/OMNIGENE/FRAMEWORK/ANALYSIS";

eval "use AnalysisProxy";
die "\n Couldn't include module AnalysisProxy!\n".
	"Please, define enviropment variable OMNIGENE_HOME!\n\n" if $@;

use ParmInfo;
use JobInfo;
use TaskInfo;
	
use Data::Dumper;

my $path = '/tmp';
my ($job_info, $rv, $tasks, $status, $s_count, $f_count);
my $job_id = 16;

my $proxy = new AnalysisProxy(
	proxy => 'http://jforge.net:8080/axis/servlet/AxisServlet',
	#debug => 1,
);

print "\nTest Analysis Proxy ...\n";
print "------------------------------------\n";

my $job_id = 254;

print "\n\n2) Test checkStatus() for job [$job_id] ...\n";
($status, $rv) = $proxy->checkStatus($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors(); 
	#$f_count++;
	print "\n... Fault\n";
} else {
	print "\nJob [$job_id] status is :$status->{status}. \n";
	print "\tDate submitted: $status->{dateSubmitted}, \n" 
							if defined $status->{dateSubmitted};
	print "\tDate complited: $status->{dateCompleted}\n"
							if defined $status->{dateCompleted};
	print "\n... Done\n";
	#$s_count++
}

print "\n\n3) Test getResults() for job [$job_id] ...";
($job_info, $rv) = $proxy->getResults($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors();
	#$f_count++;
	print "\n... Fault\n";
} else {
	print "\n".Dumper($job_info)."\n";
	print "\n... Done\n";
	#$s_count++
}	

print "\n\n4) Test getResultFiles() for job [$job_id] ...";
($job_info, $rv) = $proxy->getResultFiles($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors();
	#$f_count++;
	print "\n... Fault\n";
} else {
	#print "\n".Dumper($job_info)."\n";
	for my $file (@$job_info) {
		print "\nFile name: $file->{filename}\n";
		print "\tData: $file->{dataHandler}\n";
	}

	print "\n... Done\n";
	#$s_count++;
}	

=pod

print "\nSERVER PING [".$proxy->get_proxy()."] :".($proxy->ping)[0]."\n\n";

print "\nTest submitJob() ... \n";
my $taskID = 1;
my $file = shift || 'text.txt';

unless(-e "$path/$file") { 
	open(FILE, ">$path/$file") or 
						die "Cannot create file $file in the $path: $!!";
	print FILE "test\n";					
	print FILE "AGCTTTTCATTCTGACTGCAACGGGCAATATGTCTCTGTGTGGATTAAAAAAAGAGTGTCTGATAGCAGC".
                    "TTCTGAACTGGTTACCTGCCGTGAGTAAATTAAAATTTTATTGACTTAGGTCACTAAATACTTTAACCAA".
                   "TATAGGCATAGCGCACAGACAGATAAAAATTACAGAGTACACAACATCCATGAAACGCATTAGCACCACC\n";					
	close FILE;		
}

my $parInfo1 = new ParmInfo(name 			=> '-d', 
							value 			=> 'nr(nucleotide);nr(peptide);htg;Mitog;gbhomosapiens;Ecoli', 
							'description' 	=> 'My blast job 1', 
							inputFile 		=> 'false',
							outputFile		=> 'false',
							label			=> 'Database',
							attributes		=> {'LABEL' => 'Database'} );

my $parInfo2 = new ParmInfo(name 			=> '-p', 
							value 			=> 'blastn;blastp', 
							'description' 	=> 'My blast job 2', 
							inputFile 		=> 'false',
							outputFile		=> 'false',
							label			=> 'Program Name',
							attributes		=> {'LABEL' => 'Program Name'} );

my $parInfo3 = new ParmInfo(name 			=> '-e', 
							value 			=> '0.001;0.01;0.1;1;10.0;100.0;1000.0', 
							'description' 	=> 'My blast job 3', 
							inputFile 		=> 'false',
							outputFile		=> 'false',
							label			=> 'Expectation Value',
							attributes		=> {'LABEL' => 'Expectation Value'} );

my $parVal4 = qq{
	pairwise;query-anchored showing identities;query-anchored no identities;
	flat query-anchored show identities;flat query-anchored no identities;
	query-anchored no identities and blunt ends;flat query-anchored no identities 
	and blunt ends;XML Blast output;tabular;tabular with comment lines };

my $parInfo4 = new ParmInfo(name 			=> '-m', 
							value 			=> $parVal4, 
							'description' 	=> 'My blast job 4', 
							inputFile 		=> 'false',
							outputFile		=> 'false',
							label			=> 'Alignment View Option',
							attributes		=> {'LABEL' => 'Alignment View Option'} );

($job_info, $rv) = $proxy->submitJob($taskID, 
			[ $parInfo1->build_soap_data, $parInfo2->build_soap_data, 
				$parInfo3->build_soap_data, $parInfo4->build_soap_data ], $file, $path);

if( $rv < 0 ) { 
	warn $proxy->get_errors; 
	print "\n... Fault\n";
} else {
	print "\nJob ID [".$job_info->get_jobNumber."]: ".$job_info->get_dateSubmitted." - ".$job_info->get_status."\n";

	my $i=0;
	print "\tJob parameters:\n";
	for my $par (@{ $job_info->get_parameterInfoArray() }) {
		print "\t\t".++$i.") ".$par->get_name.': '.$par->get_description."\n";
	}
	
	print "\n... Done\n";
}	

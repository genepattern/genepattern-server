#!/usr/bin/perl -w

#######################################################################
#$Id$
#
##
# 	Script for testing AnalysisProxy.
# For correct work you should define environment 
# variable OMNIGENE_HOME because it needs for 
# include OAE perl modules.
#
# Also, for test of method submitJob() is created a file 
# in the directory /tmp.
# 
# Developed by Michael Stepanov at 07/10/2003.
#
# Email: frezza@narod.ru, michael_stepanov@hotmail.com 
#
######################################################################

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

print "\nSERVER PING [".$proxy->get_proxy()."] :".($proxy->ping)[0]."\n\n";

$s_count = 0;
$f_count = 0;

($tasks, $rv) = $proxy->getTasks();
if( $rv < 0 ) { 
	warn $proxy->get_errors();
	print "\n... Fault\n";
	$f_count++;
} else {
	print "\n\n1) Test getTask ...\n";
	print "--- Tasks list ---\n";
	print join("\t\t", qw/ID Name Description ClassName/)."\n";
	#my $count = 0;
	for my $task (@$tasks) {
		print join("\t", $task->get_ID, $task->get_name, 
										$task->get_description, $task->get_taskClassName)."\n";
	}	
	print "\n... Done\n";
	$s_count++;
}

print "\n\n2) Test checkStatus() for job [$job_id] ...\n";
($status, $rv) = $proxy->checkStatus($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors(); 
	$f_count++;
	print "\n... Fault\n";
} else {
	print "\nJob [$job_id] status is :$status->{status}. \n";
	print "\tDate submitted: $status->{dateSubmitted}, \n" 
							if defined $status->{dateSubmitted};
	print "\tDate complited: $status->{dateCompleted}\n"
							if defined $status->{dateCompleted};
	print "\n... Done\n";
	$s_count++
}

print "\n\n3) Test getResults() for job [$job_id] ...";
($job_info, $rv) = $proxy->getResults($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors();
	$f_count++;
	print "\n... Fault\n";
} else {
	print "\n".Dumper($job_info)."\n";
	print "\n... Done\n";
	$s_count++
}	

print "\n\n4) Test getResultFiles() for job [$job_id] ...";
($job_info, $rv) = $proxy->getResultFiles($job_id);

if( $rv < 0 ) { 
	warn $proxy->get_errors();
	$f_count++;
	print "\n... Fault\n";
} else {
	#print "\n".Dumper($job_info)."\n";
	for my $file (@$job_info) {
		print "\nFile name: $file->{filename}\n";
		print "\tData: $file->{dataHandler}\n";
	}

	print "\n... Done\n";
	$s_count++;
}	

print "\n\n5) Test submitJob() ... \n";
my $taskID = 0;
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

#my $parInfo = new ParmInfo(name => '-d', value => 'Ecoli', 'description' => 'My test');
my $parInfo1 = new ParmInfo(name 			=> '-f1', 
							value 			=> $file, 
							'description' 	=> 'My test 1', 
							inputFile 		=> 'true',
							outputFile		=> 'true',
							attributes		=> {'MODE' => 'IN', 'TYPE' => 'FILE'} );

my $parInfo2 = new ParmInfo(name 			=> '-f2', 
							value 			=> $file, 
							'description' 	=> 'My test 2', 
							inputFile 		=> 'true',
							attributes		=> {'LABEL' => 'Database'} );

($job_info, $rv) = $proxy->submitJob($taskID, 
			#[['-d', 'Ecoli', 'job'], ['-p', 'blastn', 'job'], ['-e', '10.0', 'job']], 
			#[['-f1', $file, 'My test Job 1'], ['-f2', $file, 'My test Job 2']],
			[ $parInfo1->build_soap_data, $parInfo2->build_soap_data ],
			$file, $path);

if( $rv < 0 ) { 
	warn $proxy->get_errors; 
	$f_count++;
	print "\n... Fault\n";
} else {
	#print Dumper($job_info);
	print "\nJob ID [".$job_info->get_jobNumber."]: ".$job_info->get_dateSubmitted." - ".$job_info->get_status."\n";

	my $i=0;
	print "\tJob parameters:\n";
	for my $par (@{ $job_info->get_parameterInfoArray() }) {
		print "\t\t".++$i.") ".$par->get_name.': '.$par->get_description."\n";
	}
	
	print "\n... Done\n";
	$s_count++;
}	

print "\n------------------------------------------\n";
print "\nEnd of Analysis Proxy test: \n";
print "\t\t$s_count successful; $f_count fault!";

#print "\n Delete an example file ...";


#$Id$

=head2 NAME

JobInfo.pm

=head2 SYNOPSYS

use JobInfo;

my $job = new JobInfo();

=head2 DESCRIPTION

Class JobInfo implements a logic for Task Job info.

=cut

package JobInfo;

use strict;

use fields qw( 	status 
				userId 
				dateSubmitted 
				taskID 
				dateCompleted 
				inputFileName
                jobNumber
                parameterInfo  
				resultFileName
                parameterInfoArray
			);

use vars qw( %FIELDS $AUTOLOAD );

use lib "$ENV{OMNIGENE_HOME}/languages/perl/src/OMNIGENE/FRAMEWORK/ANALYSIS";

use InfoBaseClass;

use Data::Dumper;

use base qw( InfoBaseClass );

{
	my $_def_props = {
		status				=> undef,
        userId				=> '',
        dateSubmitted		=> undef,
        taskID				=> undef,
        dateCompleted		=> undef,
        inputFileName		=> '',
        jobNumber			=> undef,
        parameterInfo		=> '',
		resultFileName		=> undef,
        parameterInfoArray	=> [],                       	
	
	};

	sub _def_props { $_def_props }
}

sub _init {
	my $self = shift;
	my %args = @_;

	print "[JobInfo]: init ...\n";

	my $def_val = _def_props();

	for my $attr (sort keys %FIELDS) {
		my $method = "set_$attr";
		my $value = exists $args{$attr} ? $args{$attr} : $def_val->{$attr};
		$self->$method($value);
	}
}

sub DESTROY {
	my $self = shift;
}

END { }

1;

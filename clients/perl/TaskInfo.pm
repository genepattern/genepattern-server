#$Id$

=head2 NAME

TaskInfo.pm

=head2 SYNOPSYS

use TaskInfo;

my $job = new TaskInfo();

=head2 DESCRIPTION

Class TaskInfo implements a logic for Analysis Tasks.

=cut

package TaskInfo;

use strict;

use fields qw( 	ID
				parameterInfo
				userId
				taskInfoAttributes
				name
				accessId
				taskClassName				
                parameterInfoArray
				description				
			);

use vars qw( %FIELDS $AUTOLOAD );

use lib "$ENV{OMNIGENE_HOME}/languages/perl/src/OMNIGENE/FRAMEWORK/ANALYSIS";

use InfoBaseClass;

use Data::Dumper;

use base qw( InfoBaseClass );

{
	my $_def_props = {
		ID							=> undef,
        parameterInfo               => '',
        userId                      => undef,
        taskInfoAttributes          => undef,
        name                        => '',
        accessId                    => undef,
        taskClassName				=> undef,
        parameterInfoArray          => {},
        description				    => '',
	};

	sub _def_props { $_def_props }
}

sub _init {
	my $self = shift;
	my %args = @_;

	print "[TaskInfo]: init ...\n";

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

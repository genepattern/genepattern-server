#$Id$

=head2 NAME

ParmInfo

=head2 SYNOPSYS

use ParmInfo;

my $params = new ParmInfo();

=head2 DESCRIPTION

Class ParmInfo implements a logic for build ParmInfo SOAP parameters.

=cut

package ParmInfo;

use strict;

use fields qw( outputFile value name label inputFile description attributes );

use vars qw( %FIELDS $AUTOLOAD );

use lib "$ENV{OMNIGENE_HOME}/languages/perl/src/OMNIGENE/FRAMEWORK/ANALYSIS";

use InfoBaseClass;
use SOAP::Lite;

use Data::Dumper;

use base qw( InfoBaseClass );

{
	my $_def_props = {
		'outputFile' 	=> 'false',
		'value' 		=> '',
		'name' 			=> '',
		'inputFile' 	=> 'false',
		'label' 		=> '',
		'description' 	=> 'Job',
		'attributes' 	=> {},                                                 	
	
	};

	sub _def_props { $_def_props }
}

sub _init {
	my $self = shift;
	my %args = @_;

	print "[ParmInfo]: init ...\n";

	my $def_val = _def_props();

	for my $attr (sort keys %FIELDS) {
		my $method = "set_$attr";
		my $value = exists $args{$attr} ? $args{$attr} : $def_val->{$attr};
		$self->$method($value);
	}
}

sub build_soap_data {
	my $self = shift;
	my $name_space = 'ns1';
	my @attrs;

	my $attributes = $self->get_attributes();

	for my $attr ( %$attributes ) {	
		my $item = SOAP::Data->name('item' => \SOAP::Data->value(	
						SOAP::Data->name('key' 	=> $attr),    
						SOAP::Data->name('value'=> $attributes->{$attr}) ) );
		push @attrs, $item;				
	}
	
	my $parmInfo = SOAP::Data->new(name => 'item', type => "$name_space:ParmInfo", value => \SOAP::Data->value(
						SOAP::Data->new(name => 'attributes', type => "ns2:Map", attr => {"xmlns:ns2" => 'http://xml.apache.org/xml-soap'}, 
													value => \SOAP::Data->value( @attrs  ) ),
						SOAP::Data->name('description' 	=> $self->get_description),
						SOAP::Data->name('inputFile'	=> $self->get_inputFile),
						SOAP::Data->name('label'		=> $self->get_label),
						SOAP::Data->name('name'			=> $self->get_name),
						SOAP::Data->name('outputFile'	=> $self->get_outputFile),
						SOAP::Data->name('value'		=> $self->get_value)
					)
				);
	return $parmInfo;			
}

sub DESTROY {
	my $self = shift;
	#warn "\nDestroy object ParmInfo\n";
}

1;

#$Id$

=head2 NAME

AnalysisProxy.pm

=head2 SYNOPSYS

use AnalysisProxy;

# Create a proxy object with default parameters
my $proxy = new AnalysisProxy()

# Define connect parameters
my $proxy = new AnalysisProxy(
	proxy 	=> 'http://localhost:9090/axis/servlet/AxisServlet',
	uri		=> 'Analysis1',
	debug	=> 1
);

my $tasks_info = $proxy->getTasks();

=head2 DESCRIPTION

This module is a proxy to the OAE toolkit.

=cut

package AnalysisProxy;

use strict;

#use SOAP::Lite;# +trace => 'debug';
use MIME::Entity 7.33 ();
use Data::Dumper;
#use SOAP::MIME;

use vars qw/ %FIELDS $AUTOLOAD /;
use fields qw/ proxy uri soapObj debug errors /;

{	
	my $_defaults = {
		proxy	=> "http://localhost:8080/axis/servlet/AxisServlet",
		uri		=> "Analysis",
		debug	=> 0,
		soapObj	=> undef,
		errors	=> undef,
	};

	sub _defaults { $_defaults }
}

=item new()

Object constructor

=cut
sub new {
    my $self = shift;
    my $class = ref($self) || $self;
    $self = {};    
	
	my %defaults = %{ _defaults() };
	
	for my $item (keys %defaults) {
		$self->{$item} = $defaults{$item};
	}
	
    bless($self, $class);
	$self->_init(@_);
    return $self;
}

=item _init()

Initialize a new object

=cut
sub _init {
	my $self 	= shift;
	my %params 	= @_;

	for my $par (keys %params) {
		$self->{$par} = $params{$par}
					if exists $FIELDS{$par};
	}

	if($self->get_debug) {
		eval "use SOAP::Lite +trace => 'debug'";
	} else {
		eval "use SOAP::Lite;";
	}

	die "$@!" if $@;

	my $soap = SOAP::Lite 
					->uri( $self->get_uri() )
    				->on_action( sub { return $self->get_uri() } )					
    				->proxy( $self->get_proxy() );

	$self->set_soapObj($soap);				
}
=pod
sub soapObj {
    my $self = shift;
    if(!$self->{_URL}) { die "must specify an endpoint address!\n" }
    return SOAP::Lite->service($self->{_URL});
}
=cut    

=item getTasks()

Return an information about existing tasks as array reference:

	task_name, task_description, parameterInfo and task_class.

=cut
sub getTasks{
    my $self = shift;
	my $tasks_info = [];
	my $tasks_list;
	my $soap = $self->get_soapObj();
	
	die "Cannot create a soap object:$!!" 
									unless ref($soap);
	
	my $res = $soap->getTasks();

	unless($res->fault) { $tasks_list = $res->result }
	else {
		$self->errors($res);
		return (undef, -1);
	}
    
    #for my $task (@$tasks_list){
	#	push @$tasks_info, 
	#		[ $task->{name}, $task->{description}, $task->{taskClassName}];
	#
    #}
	
	return ($tasks_list, 1);
}

=item checkStatus(<Job ID>)

Return an information about status of specified job.
	Input: 	Job ID
	Output: Status of job as number. Description of
			this result store into hash 'job_stat_descr'.

=cut
sub checkStatus{
    my ( $self, $job_id ) = @_;
	
    my $soap = $self->get_soapObj();
    my $result = $soap->checkStatus( SOAP::Data->new(name => 'arg0', value => $job_id, type => 'int') );
	
	if($result->fault) {
		$self->errors($result);
		return (undef, -1);
	}	
	
	return ($result->result, 1);
}

=item getResults

Return a result of execute of specified job.

=cut
sub getResults{
    my ( $self, $job_id ) = @_;
	
    my $soap = $self->get_soapObj;	
    my $result = $soap->getResults($job_id);    

	if($result->fault) {
		$self->errors($result);
		return (undef, -1);
	}
	
	return ($result->result, 1);
}

=item getResultFiles

Return result files for specified job.

=cut
sub getResultFiles{
    my ( $self, $job_id ) = @_;
	
    my $soap = $self->get_soapObj;	
    my $result = $soap->getResultFiles($job_id);    

	if($result->fault) {
		$self->errors($result);
		return (undef, -1);
	}
	
	return ($result->result, 1);
}


=item

This method permit to submit job with specified parameters.

=cut
sub submitJob {
    my ( $self, $taskID, $args, $fname, $path ) = @_;

	local $MIME::Entity::BOUNDARY_DELIMITER="\r\n";

	my @parmInfoArray = ();
	
	my $count = 1;	

	my $cid = 'Attachment_file';
	my $name_space = 'ns1';
	
	my $attachment = MIME::Entity->build(
			Type 		=> "text/plain",
			Encoding 	=> "base64",
			Path 		=> "$path/$fname", 
			File		=> $fname,
			'Content-Id'=> "<$cid>",
			Disposition => "attachment",
		);
=item		
	for my $item (@$args) {
		my ($name, $value, $descr) = @$item;
		
		my $parmInfo = SOAP::Data->new(name => 'item', type => "$name_space:ParmInfo", value => \SOAP::Data->value(
						SOAP::Data->new(name => 'attributes', type => "ns2:Map", attr => {"xmlns:ns2" => 'http://xml.apache.org/xml-soap'}, value => \SOAP::Data->value(
							SOAP::Data->name('item' => \SOAP::Data->value(
								SOAP::Data->name('key' 	=> 'MODE'),
								SOAP::Data->name('value'=> 'IN') ) ),
							SOAP::Data->name('item' => \SOAP::Data->value(
								SOAP::Data->name('key' 	=> 'TYPE'),
								SOAP::Data->name('value'=> 'FILE') ) ),
						) ),
						SOAP::Data->name('description' 	=> $descr),
						SOAP::Data->name('inputFile'	=> 'true'),
						SOAP::Data->name('label'		=> ''),
						SOAP::Data->name('name'			=> $name),
						SOAP::Data->name('outputFile'	=> 'false'),
						SOAP::Data->name('value'		=> $value)
					)
				);
		push @parmInfoArray, $parmInfo;		
		$count++;
	}			
=cut
	my @args = (SOAP::Data->new(name => 'arg0', value => $taskID, type 	=> 'int'),
				#SOAP::Data->new(name => 'arg1', value => [ @parmInfoArray ], attr => {'xmlns:soapenc' => 'http://schemas.xmlsoap.org/soap/encoding'} ),
				SOAP::Data->new(name => 'arg1', value => [ @$args ], attr => {'xmlns:soapenc' => 'http://schemas.xmlsoap.org/soap/encoding'} ),
				SOAP::Data->new(name => 'arg2', type => "ns3:Map", attr => {"xmlns:ns3" => 'http://xml.apache.org/xml-soap'}, value => \SOAP::Data->value(
						SOAP::Data->name(item => \SOAP::Data->value(
							SOAP::Data->new(name => 'key', type => 'string', value => $fname),
							SOAP::Data->new(name => 'value', attr => {href => "cid:$cid"}) ) )
						) ) 
			);	
	
	my $method = SOAP::Data->name('submitJob')
					->prefix($name_space)		
					->uri($self->{uri})
					->encodingStyle('http://schemas.xmlsoap.org/soap/encoding');
	
	my $soap = $self->get_soapObj();
	$soap->parts( $attachment );

	my $result = $soap->call($method => @args);

	if($result->fault) {
		$self->errors($result);
		return (undef, -1);
	}

	return ($result->result, 1);	  
}

=item ping

Just make a simple ping of server.

=cut
sub ping {
	my $self = shift;
	my $soap = $self->get_soapObj();
	my $result = $soap->ping();

	if($result->fault) {
		$self->errors($result);
		return (undef, -1);
	}

	return ($result->result, 1);
}

=item errors()

=cut
sub errors {
	my ( $self, $soapErrObj ) = @_;
	
	#if(defined $soapErrObj) {	
		my $errString;
		my $soap = $self->get_soapObj();

		if(ref $soapErrObj) {
			my @str = split /:/, $soapErrObj->faultstring;
			$errString = "SOAP ERROR [".$soapErrObj->faultcode."]: \n\t".join(":\n\t", @str); 
		} else {
			$errString = "Server ERROR: ".$soap->transport->status;
		}

		$self->set_errors($errString);
	#}
	
	#return $self->get_errors();
}

=item AUTOLOAD

Implements dynamically builded methods for access to the object properties.

=cut
sub AUTOLOAD {
	my $self = shift;
	
	my ($class, $method) = split /::/, $AUTOLOAD;

	# Implement methods for access to the object properties 
	if($method =~ /^get_(.*)/) {
		return $self->{$1} if exists $FIELDS{$1};
	}

	if( $method =~ /^set_(.*)/ && @_ && exists $FIELDS{$1} ) {
		$self->{$1} = shift;
		return $self->{$1}; 
	}

	die "Method [$method] not found in the class [$class]!";
}

sub DESTROY {
	my $self = shift;	
}

END { }

1;

=head2 AUTHOR

Developed by Michael Stepanov at 07/10/2003.

Email: I<frezza@narod.ru>, I<michael_stepanov@hotmail.com>

=cut

	

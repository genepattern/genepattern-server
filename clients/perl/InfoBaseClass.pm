package InfoBaseClass;

use strict;

use vars qw( %FIELDS $AUTOLOAD );
use fields qw();

sub new {
	my $self = shift;
    my $class = ref($self) || $self;
    #my %params = (@_);
    $self = {};    
	
#	my %defaults = %{ _defaults() };
#	for my $item (keys %defaults) {
#		$self->{$item} = $defaults{$item};
#	}
	
    bless($self, $class);
	$self->_init(@_);
    return $self;
}

sub _init {
	my $self = shift;
	print "[InfoBaseClass]: Init ...\n";	
	return 1;
}

sub AUTOLOAD {
	my $self = shift;
	my ($class, $method) = split /::/, $AUTOLOAD;

	my $fields =  eval('\%'.$class.'::FIELDS');
	
	if($method =~ /^get_(.*)/) {
		if(exists $fields->{$1} ) {
			return $self->{$1}
		} else {	
			die "Attribute [$1] is not found in the class [$class]!";
		}	
	} elsif($method =~ /^set_(.*)/) {
		if(exists $fields->{$1}) {
			$self->{$1} = shift();
			return $self->{$1};
		} else {
			die "Attribute [$1] is not found in the class [$class]!";
		}
	} else {
		die "Method [$method] not found in the class [$class]!";
	}
}

1;

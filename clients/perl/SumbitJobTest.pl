#!/usr/bin/perl -w

use SOAP::Lite +trace => 'debug';
use MIME::Entity;
use IO::File;

##
# Test submit job with SOAP::Lite
##

my ($file1, $file2) = @ARGV;

my $ent1 = build MIME::Entity
    Type => "text/plain",
    Encoding => "binary",
    Path => $file1, 
    'Content-Id' =>'<File1>',
    Disposition => "attachment";

my $ent2 = build MIME::Entity
    Type => "text/plain",
    Encoding => "binary",
    Path => $file2, 
    'Content-Id' =>'<File2>',
    Disposition => "attachment";

my $soap = SOAP::Lite
    ->uri("Analysis")
    ->on_action(sub {return "Analysis"})
    ->proxy("http://localhost:9090/axis/servlet/AxisServlet")
    #->parts([$ent1, $ent2])
    ->submitJob(SOAP::Data->name("name" => 0)
		->type("xsd:int"),
	      SOAP::Data->name("parameters" =>[
			       
					       \SOAP::Data->value(SOAP::Data->name("description" => "input1"),
								SOAP::Data->name("inputFile" => "true")->type("xsd:boolean"),
								SOAP::Data->name("label" => ""),
								SOAP::Data->name("name" => "-f1"),
								SOAP::Data->name("outputFile" => "false")->type("xsd:boolean"),
								SOAP::Data->name("value" => $file1)),
					       
					       \SOAP::Data->value(SOAP::Data->name("description" => "input1"),
								SOAP::Data->name("inputFile" => "true")->type("xsd:boolean"),
								SOAP::Data->name("label" => ""),
								SOAP::Data->name("name" => "-f2"),
								SOAP::Data->name("outputFile" => "false")->type("xsd:boolean"),
								SOAP::Data->name("value" => $file2)) 
					      ])
		
		
		);

								   
		
	       


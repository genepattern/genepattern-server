#!/usr/bin/perl -w

use SOAP::Lite;

#Hacked to support Attachments below
use SOAP::MIME;
#need this to build MIME::Entities
use MIME::Entity;


#Need this line to make sure that the attachments are properly separated out and recognized by Axis
BEGIN {$MIME::Entity::BOUNDARY_DELIMITER = "\r\n";}

#start the code
#get some files for testing
my ($file1, $file2) = @ARGV;

my $ent1 = build MIME::Entity
    Type => "text/plain",
    Encoding => "base64",
    Path => $file1, 
    Id => 'cid:8AD4BDB760E6118542CD87DD2CFB865C',
    Disposition => "attachment";
    

my $ent2 = build MIME::Entity
    Type => "text/plain",
    Encoding => "base64",
    Path => $file2,
    Id => 'cid:58F0C854A225835E07DE1A757DFE2357',
    Disposition => "attachment";

push @parts, $ent1;
push @parts, $ent2;


my $soap = SOAP::Lite
    ->uri("Analysis")
    ->on_action(sub {return "Analysis"})
    ->proxy("http://localhost:9090/axis/servlet/AxisServlet")
    ->parts(@parts)
    ->submitJob(
          SOAP::Data->name("arg0" => 0)->type("xsd:int"),
          SOAP::Data->type('SOAP-ENC:Array')
        ->attr({ 'SOAP-ENC:arrayType' => 'namesp1:ParmInfo[1]'})
        ->name("arg1" =>
               \SOAP::Data->value(createItem('inputfile1', 'true', '', '-f1', 'false', 'test1.txt'),
                      createItem('inputfile2', 'true', '', '-f2', 'false', 'test2.txt'))),
          SOAP::Data->name("arg2" => 
                   \SOAP::Data->value(createKeyHref('test1.txt', 'cid:8AD4BDB760E6118542CD87DD2CFB865C'),
                          createKeyHref('test2.txt', 'cid:58F0C854A225835E07DE1A757DFE2357')
                          ))
        ->type('ns4:Map')
        ->attr({'xmlns:ns4' => "http://xml.apache.org/xml-soap"}));


sub createItem {
    my ($d, $i, $l, $n, $o, $v) = @_;
    return SOAP::Data->name('item' => 
                \SOAP::Data->type('namesp2:Map')
                ->attr({'xmlns:namesp2' => "http://xml.apache.org/xml-soap"})
                ->name("attributes" =>
                   \SOAP::Data->value(createKeyValue('MODE', 'IN'),
                              createKeyValue('TYPE', 'FILE')),
                 SOAP::Data->name("description" => $d)->type('xsd:string'),
                 SOAP::Data->name("inputFile" => $i)->type('xsd:boolean'),
                 SOAP::Data->name("label" => $l)->type('xsd:string'),
                 SOAP::Data->name("name" => $n)->type('xsd:string'),
                 SOAP::Data->name("outputFile" => $o)->type('xsd:boolean'),
                 SOAP::Data->name("value" => $v)->type('xsd:string'),
                   )
                );
}


sub createKeyValue {
    my ($key, $value) = @_;
    return SOAP::Data->name('item' => \SOAP::Data->value(
                               SOAP::Data->name('key' => $key),
                               SOAP::Data->name('value' => $value),
                             ));
}

sub createKeyHref {
    my ($key, $href) = @_;
    return SOAP::Data->name('item' => \SOAP::Data->value(
                               SOAP::Data->name('key' => $key),
                               SOAP::Data->name('value')->attr({'href' => $href}),
                             ));
}
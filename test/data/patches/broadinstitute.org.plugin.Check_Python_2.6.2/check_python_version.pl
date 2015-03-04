#!/usr/bin/perl

use Config;
use File::Path;
use IPC::Open3;
use IO::Select;
use IO::Handle;

print "\nChecking Python is installed and that the version is greater than or equal to 2.6\n";

$python_test = system("python");

if($python_test != 0)
{
    print STDERR "An error occurred while executing python command. Please check that Python 2.6 or greater is installed and on the system path";
    exit(1);
}

print "\nPython installed. Now checking version is 2.6 or greater";
#python is installed now check the version
$versionString = getVersionString();

print "\nPython version information found: ".$versionString;

@versionArray = split(' ', $versionString);
#the second item in the list is the version number
$versionArrayLength = @versionArray;

$skipped = 0;
#if version length is not at least 2 then skip the version
#check since this is not expected

if($versionArrayLength >= 2)
{
    @versionNumArray = split(/\./, $versionArray[1]);
    $versionNumArrayLength = @versionNumArray;

    if($versionNumArrayLength > 1)
    {
        if( $versionNumArray[0] =~ /^-?\d+$/)
        {
            $num = int($versionNumArray[0]);
            if($num < 2)
            {
                print STDERR "\nPython version must be 2.6 or greater. Instead found version ".$versionString;

                exit(1);
            }
        }
        else
        {
            $skipped = 1;
        }

    }

    if($versionNumArrayLength > 2)
    {
        if($versionNumArray[1] =~ /^-?\d+$/)
        {
            $num = int($versionNumArray[1]);

            if($num < 4)
            {
                print STDERR "\nPython version must be 2.6 or greater. Instead found version ".$versionString;
                exit(1);
            }
        }
        else
        {
            $skipped = 1;
        }
    }
}
else
{
    $skipped = 1;
}

if($skipped)
{
    print "\nSkipping check of Python version: could not verify the Python version";
}
else
{
    print "\nVerified Python version is 2.6 or greater";
}

sub getVersionString
{
    $cmd="python -V";
    $versionString = "";
    $Pin  = new IO::Handle;       $Pin->fdopen(10, "w");
    $Pout = new IO::Handle;       $Pout->fdopen(11, "r");
    $Perr = new IO::Handle;       $Perr->fdopen(12, "r");
    $Proc = open3($Pin, $Pout, $Perr, $cmd);

    my $sel = IO::Select->new();
    $sel->add($Perr, $Pout);

    while (my @ready = $sel->can_read)
    {
        foreach my $handle (@ready)
        {

            # process has printed something on standard out
            my ($count, $data);
            $count = sysread($handle, $data, 1024);
            if ($count == 0)
            {
                $sel->remove($handle);
                next;
            }

            $versionString .= $data;
        }
    }

    close($Perr);
    close($Pout);

    waitpid($Proc, 0);

    return $versionString;
}

exit(0);


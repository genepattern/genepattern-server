Test scripts for the RNA-seq modules.

The test scripts are twill scripts (http://twill.idyll.org/), invoked with the twill-sh and twill-fork commands.
To use twill on a linux login server, e.g. ssh gpdev02
    use Python-2.5
    twill-sh

To install twill on Mac OS X, 
    sudo easy_install twill

To run the tests you must first create your own test.properties.twill file, based on a copy of the test.properties-sample.twill file.

Example test using twill-sh (single run):

Example test using twill-fork (simulate 4 concurrent users):
    twill-fork -n 4 -p 4 -u http://genepatterntest.broadinstitute.org run_bowtie.aligner.indexes.twill


1) Developing twill scripts for testing GenePattern modules.
    The basic pattern is simple: Login to a server. Open the Run modules page for the module you want to test. Populate the input form. Submit the form. Validate.
    At the moment validation is done manually by logging into the server and looking at the job results page. This can be improved.




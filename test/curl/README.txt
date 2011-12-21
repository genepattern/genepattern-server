Example curl scripts/commands for interacting with the GP server.

1) To login to a GP server
curl --cookie localhost.cookies --cookie-jar localhost.cookies -d "username=test&password=test" http://127.0.0.1:8080/gp/login
curl --cookie gptest.cookies --cookie-jar gptest.cookies -d "username=test&password=test" http://genepatterntest.broadinstitute.org/gp/login
curl --cookie gpprod.cookies --cookie-jar gpprod.cookies -d "username=test&password=test" http://genepattern.broadinstitute.org/gp/login

2) To fetch a file from the jobResults
curl --cookie gptest.cookies --cookie-jar gptest.cookies --dump-header gptest.headers http://genepatterntest.broadinstitute.org/gp/jobResults/40767/stderr.cvt.txt

3) To fetch just the header
curl --head --cookie gptest.cookies --cookie-jar gptest.cookies --dump-header gptest.headers http://genepatterntest.broadinstitute.org/gp/jobResults/40767/stderr.cvt.txt


# Test case 1, downloading large stderr.txt file from gpdev (9589) causes browser to crash 
Let's compare the headers from the following servers:
    gpprod
    gpdev
    localhost

To download a *.txt file from gpprod, do the following:
curl --cookie gpprod.cookies --cookie-jar gpprod.cookies -d "username=test&password=test" http://genepattern.broadinstitute.org/gp/login
curl --head --cookie gpprod.cookies --cookie-jar gpprod.cookies http://genepattern.broadinstitute.org/gp/jobResults/352845/gp_execution_log.txt


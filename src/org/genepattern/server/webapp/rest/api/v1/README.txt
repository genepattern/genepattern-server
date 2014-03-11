========================================
Using the GenePattern REST API
========================================

This document describes how to use the GenePattern REST API.
Target audience: Someone integrating the Genet portal with a REST-enabled GP server.

Setup:

Install a GP server which has support for the REST API in whatever manner seems appropriate to you.
You will need GP 3.6.0 or higher.  If not available on
http://www.broadinstitute.org/cancer/software/genepattern/installer/latest/install.htm,
contact gp-help and ask for an installer. Note: because we use HTTP 
Basic Authentication for REST calls, you should install your server as an HTTPS server.
Our online documentation has the details:
    http://www.broadinstitute.org/cancer/software/genepattern/gp_guides/administrators-guide/sections/security

After your server is up and running, you need to do some things with the web GUI. Rest calls have not
been implemented for all features. Create a user account and password. Install your module. After
you have an account and the module is installed you are ready to use the REST API to run your 
module, poll for job completion, and download job result files.


1) To run your module
1a) If necessary, upload your data files to the server. To upload a data file,
    POST {GenePatternURL}/rest/v1/data/upload/job_input?name={filename}
    The request body must contain the full content of the data file to be uploaded.
After successful upload, the server response will be 201, Created.
The URI to the uploaded file will be in the Location header of the response.
The URI will also be included in the response body.
Use this URI as the value for an input parameter when you POST a job to the server.
     
Here is the curl template:
    curl -X POST -D header.txt -u {username}:{password} --data-binary @{pathToFile} "{GenePatternURL}/rest/v1/data/upload/job_input?name={fileName}"
And the response body template:
{uriToUploadedFile}
And the response header template:
HTTP/1.1 201 Created
Location: {uriToUploadedFile}

For example, 
    curl -X POST -D header.txt -u test:**** --data-binary @all_aml_test.cls "http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input?name=all_aml_test.cls"
This assumes that you run the command from a working directory which includes the 'all_aml_test.cls' file.
Response body:
http://127.0.0.1:8080/gp/users/test/tmp/run4715867801285988204.tmp/all_aml_test.cls
Header contents:
HTTP/1.1 201 Created
Date: Fri, 29 Mar 2013 18:53:49 GMT
Server: Apache-Coyote/1.1
Accept-Ranges: bytes
Location: http://127.0.0.1:8080/gp/users/test/tmp/run4715867801285988204.tmp/all_aml_test.cls
Content-Type: text/plain
Set-Cookie: JSESSIONID=AC9D70BD53976241950C9C71A0A0F1C4; Path=/gp
Transfer-Encoding: chunked

1b) To start a job, POST a JSON representation of the job input form to the server.
    POST {GenePatternURL}/rest/v1/jobs {jobInput.json}
The Location field in the response header will be the URI for the newly created job.

The jobInput JSON representation template. You must supply actual values for the items enclosed by angle ('<' and '>') brackets. 
{
  "lsid":"<lsid>", 
  "params": [
    {"name": "<pname0>", "values": [ "<value0>" ] },
    {"name": "<pname1>", "values": [ "<value1>" ] },
    ...
    {"name": "<pnameN>", "values": [ "<valueN>" ] },
  ]
}

Use the GUI to get the template for your particular module. The easiest way to do so
is to log into your GP account, and do one run of the module. When the job is complete,
download the execution log. It includes the lsid as well as the list of parameter names.

You can also get the same information by looking at the 'Properties' for your module. From 
the GUI, select the module, which takes you to the job input form for the module. 
Click the 'Properties' link or menu item to get the details.

Here is an example jobInput.json file to run the ConvertLineEndings module using the 
data file which was uploaded in the example for step 1a.
{ "lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2", 
  "params": [
    {"name": "input.filename", "values": [ "http://127.0.0.1:8080/gp/users/test/tmp/run4715867801285988204.tmp/all_aml_test.cls" ] },
    {"name": "output.file", "values": [ "all_aml_test.cvt.cls" ] }
  ]
}
Note: the 'output.file' parameter is unnecessary, because there is a default value. It is set here for illustration.

Assuming you saved this as a file named jobInput.json, here is the example curl command line:
curl -X POST -D header.txt -u test:**** -H "Content-type: application/json" -d @jobInput.json http://127.0.0.1:8080/gp/rest/v1/jobs
The URI for the job will be included in the Location of the response header.
For example,
    http://127.0.0.1:8080/gp/rest/v1/jobs/46152

2) Getting information for a job
2a) Get the details for your job.
    GET {jobUri}
A JSON representation of the job will be included in the response body. 
The following curl command template works:
    curl -D headers.txt -u {userId}:{password} {jobUri} > jobResult.json
Look at the 'headers.txt' file to inspect the HTTP header from the response.
The response body will be saved into the jobResult.json file.

2b) To poll for job completion.
The jobResult JSON representation includes a 'status' object, which can be used to check for job completion.
Here is example json output for a job which is still running:
{
  "self":"http://127.0.0.1:8080/gp/rest/v1/jobs/46149,
  "jobId":"46149",
  "status": {
    "hasError":false,
    "isFinished":false},
  "numOutputFiles":0,
  "outputFiles":[]
}
Note: the status.isFinished is 'false'

Recommendation is to poll for job completion by calling GET {jobUri} within a loop.
Pause a second so you don't throttle the server, then GET again until isFinished is true.
(Note: we'd like to make this more RESTful by adhering to web standards to indicate when we expect the job to be complete).

2c) To check the exit status.
After the job is finished, look at the status.hasError to see if it completed successfully.

2d) To download result files.
Completed jobs have an 'outputFiles' array. This is an array of output file objects.
Each output file has a link which has an href. Get the href to download the file.

    GET {outputFiles[idx].link.href}

You must set the User-Agent to 'GenePatternRest', otherwise you will download the contents of 
the login page instead of the actual file.

Example HTTP request header:

    User-Agent: GenePatternRest

Example curl command line, 
    curl -A GenePatternRest ...

Here is an example of the json output for a job which is finished:
{
  "self":"http://127.0.0.1:8080/gp/rest/v1/jobs/46149,
  "jobId":"46149",
  "status": { 
    "isFinished":true,
    "hasError":false, 
    "executionLogLocation":"http://127.0.0.1:8080/gp/jobResults/46149/gp_execution_log.txt"
  },
  "numOutputFiles":2,
  "outputFiles": [ 
    { "lastModified":1363319243000,
      "fileLength":1732,
      "link": { 
        "name":"all_aml_train.preprocessed.feat.odf",
        "href":"http://127.0.0.1:8080/gp/jobResults/46149/all_aml_train.preprocessed.feat.odf"}}, 
    { "lastModified":1363319243000,
      "fileLength":1492,
      "link": {
        "name":"all_aml_train.preprocessed.pred.odf",
        "href":"http://gpdev.broadinstitute.org/gp/jobResults/46149/all_aml_train.preprocessed.pred.odf"}}
  ],
}

Here is an example curl command to GET a result file,
    curl -X GET -A GenePatternRest -u test:**** -O http://127.0.0.1:8080/gp/jobResults/46149/all_aml_train.preprocessed.feat.odf

* We have not tested jobSubmit for pipelines.
* We do not support getting job details for completed pipelines
* We use gp-unit as our reference implementation of a REST client. It's implemented in Java, uses the apache http client library,
  and hand-coded JSON parsing code to upload data files, run jobs, poll for job completion, and download result files.
  Contact gp-help for the details. If you have access to SVN, start here:
      https://svn.broadinstitute.org/gp2/trunk/modules/util/gp-unit
  Look at the JobRunnerRest.java class for example source code.

  

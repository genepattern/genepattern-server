https://community.apigee.com/questions/4412/any-automate-process-for-wadlswagger.html

It was not possible (within time constraints) to get swagger into the genepattern tomcat instance since we do not have a maven build and we needed to find an old version that matched our jsersey and other libraries somewhat.  Instead we use external tools to get a semi-usable api doc.

This started with uploading the wadl (http://cloud.genepattern.org/gp/rest/application.wadl) into the apimatic.io website and using that to transform it into the first version of the swagger json format.

Then it was editted significantly and saved as genepattern-swagger.json here.  To make edits start with this json file unless the API has changed significantly.

To edit the swagger json doc, use the following website
    https://www.apimatic.io/dashboard
when done, export the json back to this directory.

Finally use redoc-cli to generate a standalone html file that describes the API using the following command
    npx redoc-cli bundle -o index.html genepattern-swagger.json 

You will need to install redoc-cli with npm first of course






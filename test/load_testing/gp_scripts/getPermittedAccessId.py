# python script for testing the SOAP interface to GenePattern
#    see: AnalysisSoapBindingStub.java
#

import SOAPpy
from SOAPpy import SOAPProxy

# Example 1: ping the service
username = 'test'
password = 'test'
server = '127.0.0.1:8080/gp'

# if password is required use this format (for basic HTTP Authentication)
gpTaskIntegrator = SOAPProxy("http://"+username+":"+password+"@"+server+"/services/TaskIntegrator")

gp_constants_public = 1
gp_constants_private = 2 
# this will return 1 if the user has permission to create a public pipeline 
access_id = gpTaskIntegrator.getPermittedAccessId(gp_constants_public)
print 'access_id:',access_id




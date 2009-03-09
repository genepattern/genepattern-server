# python script for testing the SOAP interface to GenePattern
#    see: AnalysisSoapBindingStub.java
#

import SOAPpy
from SOAPpy import SOAPProxy

# Example 1: ping the service
username = 'test'
password = 'test'
server = 'localhost:8080/gp'

# if password is required use this format (for basic HTTP Authentication)
url = "http://"+username+":"+password+"@"+server+"/services/Analysis"
# else
#url = "http://"+server+"/services/Analysis"

gpAnalysis = SOAPProxy(url)
gpAnalysis.ping()



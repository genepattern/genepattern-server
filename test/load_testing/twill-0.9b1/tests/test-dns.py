import twilltestlib
from tests import url

import pkg_resources
try:
   pkg_resources.require('dnspython>=1.4')
   no_dns_python = False
except pkg_resources.DistributionNotFound:
   no_dns_python = True
   pass

def test():
    if not no_dns_python:
       twilltestlib.execute_twill_script('test-dns.twill', initial_url=url)
    

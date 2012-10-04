Test cases for EULA feature.

Note (circa Oct 2012), this test requires some manual setup. It works when using the 'test' user account on the gpdev server.

Dependencies:
1. install version 1, 2, and 3 of the testLicenseAgreement module
    lsid=urn:lsid:9090.gpdev.gpint01:genepatternmodules:812
    version 1: requires EULA, 'test' user has not agreed
    version 2: does not require EULA
    version 3: requires EULA, 'test' user has already agreed

2. create 'test' user account
3. login to the web GUI, and accept the license agreement for version 3 only    
    
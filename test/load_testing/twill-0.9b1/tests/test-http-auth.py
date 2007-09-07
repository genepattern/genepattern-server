import os
import twilltestlib
from tests import url

def test():
    twilltestlib.execute_twill_script('test-http-auth.twill', initial_url=url)

import os
import twilltestlib

from tests import url

def test():
    twilltestlib.execute_twill_script('test-find.twill', initial_url=url)

import twilltestlib
from tests import url

def test():
    twilltestlib.execute_twill_script('test-multisub.twill', initial_url=url)

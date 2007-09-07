import twill.commands
import twilltestlib
from tests import url

def test():
    twilltestlib.execute_twill_script('test-headers.twill',
                                      initial_url=url)

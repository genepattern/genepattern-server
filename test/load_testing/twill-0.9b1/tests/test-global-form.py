import twill, twilltestlib
from tests import url

def test():
    twilltestlib.execute_twill_script('test-global-form.twill',
                                      initial_url=url)

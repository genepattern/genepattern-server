import twilltestlib
from tests import url

def test():
    """
    Test the 'formfill' extension stuff.
    """
    twilltestlib.execute_twill_script('test-formfill.twill', initial_url=url)

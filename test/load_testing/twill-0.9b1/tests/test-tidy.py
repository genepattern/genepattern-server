"""
Test the utils.run_tidy function.

This doesn't test to see if tidy was actually run; all it does is make sure
that the function runs without error.
"""
import twilltestlib
from twill import utils

def setup_module():
    pass

def test():
    bad_html = """<a href="test">you</a> <b>hello."""
    (output, errors) = utils.run_tidy(bad_html)

    print output
    print errors

def teardown_module():
    pass

if __name__ == '__main__':
    setup_module()
    test()
    teardown_module()

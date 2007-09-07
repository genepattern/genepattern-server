class TwillAssertionError(Exception):
    """
    AssertionError to raise upon failure of some twill command.
    """
    pass

class TwillNameError(Exception):
    """
    Error to raise when an unknown command is called.
    """
    pass

import sys
from twill.commands import *

__gp_url__ = None

#
# Get the GenePattern URL, e.g. 'http://localhost:8080/gp'
#
def gp_url():
    return __gp_url__

#
# Login web session to genepattern server
#
def login(*args):
    global __gp_url__
    if (len(args) == 0):
        __gp_url__ = sys.argv[1]
        username = sys.argv[2]
        if (len(sys.argv) > 3):
            password = sys.argv[3]
    else:
        __gp_url__ = args[0]
        username = args[1]
        if (len(args)>2):
            password = args[2]
        else:
            password = None

    go(__gp_url__)
    url('/gp/pages/login.jsf')
    fv('loginForm', 'username', username)
    if password is not None:
        fv('loginForm', 'password', password)
    submit()
    code(200)
    url('/gp/pages/index.jsf')
    return

def logout():
    global __gp_url__
    go(__gp_url__ + '/logout')
    code(200)
    __gp_url__ = None
    return

def waitForJobCompletion(timeout):
    return



import urllib
import mechanize
br = mechanize.Browser()
br.open("http://genepatterntest.broad.mit.edu/gp/")
br.select_form(nr=0)
br["username"] = "test"
br["loginForm:password"] = "test"
r = br.submit()
data = urllib.urlencode({'jobID':'9044','cmd':'notifyEmailJobCompletion','userID':'test','userEmail':'pcarr@broad.mit.edu'})
r = br.open("http://genepatterntest.broad.mit.edu/gp/notifyJobCompletion.ajax",data)

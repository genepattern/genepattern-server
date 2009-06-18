import urllib
import mechanize

userID = 'test'
password = 'test'
userEmail = 'pcarr@broadinstitute.org'
gpServer = 'http://127.0.0.1:8080/gp/'
jobID = '98'

br = mechanize.Browser()
br.open(gpServer)
br.select_form(nr=0)
br["username"] = userID
br["loginForm:password"] = password
r = br.submit()
data = urllib.urlencode({'jobID':jobID,'cmd':'notifyEmailJobCompletion','userID':userID,'userEmail':userEmail})
r = br.open(gpServer + 'notifyJobCompletion.ajax', data)

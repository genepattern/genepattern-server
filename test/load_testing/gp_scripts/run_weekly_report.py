import sys

from twill.commands import *

# todo: use getopt to parse command line
#
# at the moment, arglist must match exactly:
# python run_report.py <url> <username> <password> <reportdate>
#
# e.g.
# python run_report.py http://genepatterntest.broadinstitute.org:8080 ted <password> 071228
#
#

go(sys.argv[1]+'/gp')
url('/gp/pages/login.jsf')
fv('loginForm', 'username', sys.argv[2])
fv('loginForm', 'password', sys.argv[3])
submit()
code(200)
url('/gp/pages/index.jsf')

#generate the weekly report
go('/gp/createReport.jsp?returnDocument=true&pdfFormat=true&reportName=GenePatternMonthlyReport.jrxml&findStart=week&findEnd=week&startdate='+sys.argv[4]+'&enddate='+sys.argv[4])

#save the report as a PDF
save_html(sys.argv[5]+'/GenePatternWeeklyReport_'+sys.argv[4]+'.pdf')

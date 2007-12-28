import sys

from twill.commands import *

# copied from run_weekly_report.py
# see that file for documentation

go(sys.argv[1]+'/gp')
url('/gp/pages/login.jsf')
fv('loginForm', 'username', sys.argv[2])
fv('loginForm', 'password', sys.argv[3])
submit()
code(200)
url('/gp/pages/index.jsf')

#generate the monthly report
go('/gp/createReport.jsp?returnDocument=true&pdfFormat=true&reportName=GenePatternMonthlyReport.jrxml&findStart=month&findEnd=month&startdate='+sys.argv[4]+'&enddate='+sys.argv[4])

#save the report as a PDF
save_html(sys.argv[5]+'/GenePatternMonthlyReport_'+sys.argv[4]+'.pdf')





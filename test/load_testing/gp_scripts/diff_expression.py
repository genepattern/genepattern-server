# step 1: login
import sys
import gp_client
from twill.commands import *

gp_client.login()

# Step 1: Preprocess Dataset, Missing required params
go(gp_client.gp_url()+'/pages/index.jsf?lsid=PreprocessDataset')
fv('taskForm', 'taskName', 'PreprocessDataset')
submit()
code(200)
url('/gp/SubmitJob')
find('The module could not be run. The following required parameters need to have values provided;')

# Step 2: Preprocess Dataset, Upload file
go(gp_client.gp_url()+'/pages/index.jsf?lsid=PreprocessDataset')
fv('taskForm', 'input.filename_cb', 'file')
formfile('taskForm', 'input.filename', 'all_aml_test.gct')
submit()
code(200)
jobNumber=url('jobNumber=([0-9]*)')
echo('jobNumber='+jobNumber)
# sleep long enough for the job to complete ...
sleep(2)
# ... then load the jobResults page and check for the output file
go(gp_client.gp_url()+'/jobResults/'+jobNumber)
find('all_aml_test.preprocessed.gct')

# Step 3: PreprocessDataset, FTP Input file
#go(gp_client.gp_url()+'/pages/index.jsf?lsid=PreprocessDataset')
#fv('taskForm', 'input.filename_cb', 'url')
#fv('taskForm', 'input.filename_url', 'ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct')
#submit()
#code(200)
# refresh until job completes (or timeout)

#notfind('Exception')
#notfind('stderr.txt')
#jobNumber=url('jobNumber=([0-9]*)')
#echo('jobNumber='+jobNumber)
#go(gp_client.gp_url()+'/jobResults/'+jobNumber)
#find('all_aml_test.preprocessed.gct')



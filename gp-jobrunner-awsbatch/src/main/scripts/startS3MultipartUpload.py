import boto3
import json
from datetime import date, datetime, timedelta
import sys, getopt

#####################################
#
# Note that the java grabs the json output from stdout so that if you are
# using this from a GP server make sure any debugging outputs go to stderr
#
#####################################


def json_serial(obj):
    """JSON serializer for objects not serializable by default json code"""

    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    raise TypeError ("Type %s not serializable" % type(obj))


def main(argv):
	# defaults for testing
	# #################
	AWS_S3_BUCKET="gp-temp-test-bucket"
	FILE_PATH_AND_NAME="foo.txt"
	CONTENT_TYPE="text/plain"
	OUTFILENAME="ff.json"
	######### end defaults ##########

	try:
		opts, args = getopt.getopt(argv, "b:k:c:f:i:") 
		 
	except: 
		print("Error") 
  	
	for opt, arg in opts:
		if opt in ['-b', '--bucket']: 
			AWS_S3_BUCKET = arg 
		elif opt in ['-k', '--key']:  
			FILE_PATH_AND_NAME = arg.strip('\"')
			KEY = arg
		elif opt in ['-c', '--content-type']:  
			CONTENT_TYPE = arg
		elif opt in ['-o', '--outfilename']:  
			OUTFILENAME = arg
		elif opt in ['-i', '--input-json']:
			with open(arg) as f:
				data = json.load(f)
			FILE_PATH_AND_NAME=data['path']
			KEY = data['path']
			AWS_S3_BUCKET = data['bucket']

 
	s3 = session.client('s3')

	print("Create upload for key: " + FILE_PATH_AND_NAME)
	upload = s3.create_multipart_upload(
	    Bucket=AWS_S3_BUCKET,
	    Key=FILE_PATH_AND_NAME,
	    Expires=datetime.now() + timedelta(days=2),
	)
	upload_id = upload["UploadId"]
	
	print(json.dumps(upload, default=json_serial))
	with open(OUTFILENAME, 'w') as outfile:
		outfile.write(json.dumps(upload, default=json_serial))


if __name__ == "__main__":
   main(sys.argv[1:])

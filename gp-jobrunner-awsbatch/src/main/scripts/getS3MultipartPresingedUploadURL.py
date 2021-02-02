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
	OUTFILENAME="out.txt"
	PART_NUM=3
	EXPIRATION_DURATION=7200 # 2 hours
	######### end defaults ##########

	try:
		opts, args = getopt.getopt(argv, "b:k:o:u:n:e:i:") 
		 
	except: 
		print("Error") 
  	
	for opt, arg in opts:
		if opt in ['-b', '--bucket']: 
			AWS_S3_BUCKET = arg 
		elif opt in ['-k', '--key']:  
			FILE_PATH_AND_NAME = arg.strip('\"')
			KEY = arg
		elif opt in ['-o', '--outfilename']:  
			OUTFILENAME = arg
		elif opt in ['-u', '--upload-id']:  
                        UPLOAD_ID = arg
		elif opt in ['-n', '--part-num']:  
			PART_NUM = int(arg)
		elif opt in ['-e', '--expiration']:
			EXPIRATION_DURATION = int(arg)
		elif opt in ['-i', '--input-json']:
			with open(arg) as f:
				data = json.load(f)
			FILE_PATH_AND_NAME=data['path']
			PART_NUM = data['partNum']
			AWS_S3_BUCKET = data['bucket']
			EXPIRATION_DURATION = data['expiration']
			UPLOAD_ID = data['uploadId']

	session = boto3.session.Session()
	s3 = session.client('s3')

	presigned_url = s3.generate_presigned_url(
	 ClientMethod="upload_part",
	 Params={
	       "Bucket": AWS_S3_BUCKET,
	       "Key": FILE_PATH_AND_NAME,
	       "UploadId": UPLOAD_ID,
	       "PartNumber": PART_NUM,
	 },
	 ExpiresIn=EXPIRATION_DURATION,  
	 HttpMethod="PUT",
	)	
		
	print(presigned_url)
	with open(OUTFILENAME, 'w') as outfile:
		outfile.write(presigned_url)
	


if __name__ == "__main__":
   main(sys.argv[1:])

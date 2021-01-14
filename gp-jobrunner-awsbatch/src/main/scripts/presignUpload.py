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
	PROFILE=None
	FILE_PATH_AND_NAME="/Users/liefeld/foo.txt"
	CONTENT_TYPE="text/plain"
	FILENAME="foo.txt"
	NUMPARTS=3
	######### end defaults ##########

	try:
		opts, args = getopt.getopt(argv, "b:p:k:c:f:n:") 
		 
	except: 
		print("Error") 
  	
	for opt, arg in opts:
		if opt in ['-b', '--bucket']: 
			AWS_S3_BUCKET = arg 
		elif opt in ['-p', '--profile']: 
			PROFILE = arg
		elif opt in ['-k', '--key']:  
			FILE_PATH_AND_NAME = arg.strip('\"')
			KEY = arg
		elif opt in ['-c', '--content-type']:  
			CONTENT_TYPE = arg
		elif opt in ['-f', '--filename']:  
			FILENAME = arg
		elif opt in ['-n', '--num-parts']:  
			NUMPARTS = int(arg)

	if PROFILE is not None:
		session = boto3.session.Session(profile_name=PROFILE)
	else:
		session = boto3.session.Session()
 
	s3 = session.client('s3')

	upload = s3.create_multipart_upload(
	    Bucket=AWS_S3_BUCKET,
	    Key=FILE_PATH_AND_NAME,
	    Expires=datetime.now() + timedelta(days=2),
	)
	upload_id = upload["UploadId"]
	upload["presignedUrls"] = []
	
	for i in range(NUMPARTS):
		presigned_url = s3.generate_presigned_url(
	   	 ClientMethod="upload_part",
	  	  Params={
	  	      "Bucket": AWS_S3_BUCKET,
	  	      "Key": FILE_PATH_AND_NAME,
	  	      "UploadId": upload_id,
	  	      "PartNumber": i+1,
	  	  },
	  	  ExpiresIn=3600,  # 1h
	  	  HttpMethod="PUT",
		)	
		
		upload["presignedUrls"].append(presigned_url)
	
	print(json.dumps(upload, default=json_serial))
	with open(FILENAME, 'w') as outfile:
		outfile.write(json.dumps(upload, default=json_serial))


if __name__ == "__main__":
   main(sys.argv[1:])

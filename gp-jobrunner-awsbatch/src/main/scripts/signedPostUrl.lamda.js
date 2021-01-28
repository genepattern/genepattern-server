const AWS = require('aws-sdk');
// if you are using an eu region, you will have to set the signature
// version to v4 by passing this into the S3 constructor -
// {signatureVersion: 'v4' }
const s3 = new AWS.S3();

exports.handler = function (event, context) { 

  
  
  const key = `${event.input.name}`
  var contentType = `${event.input.filetype}`
  var bucket = `${event.input.bucket}`
  
  if (!key) {
    console.log('key missing:')
    context.done(new Error('S3 object key missing'))
    return;
  }
  if (!contentType){
      console.log('contentType missing')
      contentType='text/plain'
  } else {
    if ("BLANK" === contentType){
        contentType = "";
    } 
  }
  if (!bucket){
      console.log('Bucket missing')
      bucket="gp-temp-test-bucket"
  } else {
      console.log("BUCKET OK")
  }
  //contentType=typeof contentType;
  
  const params = {
    'Bucket': bucket,
    'Key': key,
    ContentType: contentType
  };

  s3.getSignedUrl('putObject', params, (error, url) => {
    if (error) {
      console.log('error:', error)
      context.done(error)
    } else {
      context.done(null, {
        url: url, 
        name: key, 
        filetype: contentType
      });
    }
  })

}

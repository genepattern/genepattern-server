
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import java.io.*;
import java.util.Calendar;
import java.util.Date;


public class GetReport {

	//returnDocument=true
	//&pdfFormat=true
	//&reportName=User_Jobs_run.jrxml
	//&findStart=month&findEnd=month
	//&startdate=071015&enddate=071015
	
public static void main(String[] args) throws Exception{
	
	java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("yyMMdd");
	
	String dateMode = args[0]; // week or month
	String reportName = args[1];
	String server = args[2];
	String user = args[3];
	String password= args[4];
	
	Date today = new Date();
	if (dateMode.equals("lastMonth")){
		dateMode = "month";
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		today = cal.getTime();
	} else if (dateMode.equals("lastWeek")){
		dateMode = "week";
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.WEEK_OF_YEAR, -1);
		today = cal.getTime();
	}
	System.out.println("Report Date is " + today);

	
    //create a singular HttpClient object
       HttpClient client = new HttpClient();
        
       String loginurl=server +"/pages/login.jsf?username="+user+"&password="+password;
          
   		System.out.println("Calling " + loginurl);
        
       GetMethod method = new GetMethod(loginurl);
       method.setFollowRedirects(true);
       int responseCode = client.executeMethod(method);
       System.out.println("response " + responseCode);
       
       String reporturl = server+"/createReport.jsp?";
       String params = "returnDocument=true&pdfFormat=true&reportName="+reportName;
       	params += "&findStart="+dateMode.toLowerCase()+"&findEnd="+dateMode.toLowerCase();
   		params += "&startdate="+ dateFormatter.format(today);
   		params += "&enddate="+ dateFormatter.format(today);   
 
       method = new GetMethod(reporturl + params);
       System.out.println("now Calling " + reporturl + params);
       
       method.setFollowRedirects(true);
       
       responseCode = client.executeMethod(method);
       
       System.out.println("response " + responseCode);
       
       String outfileName;
       Header cdHeader = method.getResponseHeader("Content-disposition");
       if (cdHeader == null){
    	   outfileName = "error.html";
       } else {
    	   String hval = cdHeader.getValue();
    	   int idx = hval.indexOf("filename=");
       
    	   outfileName = hval.substring(idx+10, hval.length()-1);
       }
       
       byte[] responseBody = method.getResponseBody();
       
       File reportDir = new File("generatedReports");
       if (!reportDir.exists()) reportDir.mkdir();
       File f = new File(reportDir, outfileName);
       System.out.println("Got " + responseBody.length + " bytes");
       OutputStream os = new FileOutputStream(f);
       os.write(responseBody, 0, responseBody.length);
		
       method.releaseConnection();
       System.out.println("Wrote file " + f.getCanonicalPath());
}




}
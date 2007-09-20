package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.BaseDAO;
import org.genepattern.util.GPConstants;

public class RegisterServerBean {
	  private static Logger log = Logger.getLogger(RegisterServerBean.class);
	  
	  // not in properties file so it cannot be (easily) overridden
	  private String action="http://www.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_server_license_process.cgi";
	  
	  private String email;
	  private String name;
	  private String title;
	  private String organization;
	  private String department;
	  private String address1;
	  private String address2;
	  private String city;
	  private String state;
	  private String zipCode;
	  private String country;
	  private boolean acceptLicense = false;
	  private String os;
	  private boolean error = false;
	  
	  private boolean joinMailingList = true;
	  private String[] countries = { "United States of America", "Albania",
			"Algeria", "American Samoa", "Andorra", "Angola", "Anguila",
			"Antarctica", "Antigua and Barbuda", "Argentina", "Armenia",
			"Aruba", "Australia", "Austria", "Azerjaijan", "Bahamas",
			"Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium",
			"Belize", "Benin", "Bermuda", "Bhutan", "Bolivia",
			"Bosnia and Herzegovina", "Botswana", "Bouvet Island", "Brazil",
			"British Indian Ocean territory", "Brunei Darussalam", "Bulgaria",
			"Burkina Faso", "Burundi", "Cambodia", "Cameroon", "Canada",
			"Cape Verde", "Cayman Islands", "Central African Republic", "Chad",
			"Chile", "China", "Christmas Island", "Cocos (Keeling) Islands",
			"Colombia", "Comoros", "Congo", "Cook Islands", "Costa Rica",
			"Croatia (Hrvatska)", "Cuba", "Cyprus", "Czech Republic",
			"Denmark", "Djibouti", "Dominica", "Dominican Republic",
			"East Timor", "Ecuador", "Egypt", "El Salvador",
			"Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia",
			"Falkland Islands", "Faroe Islands", "Fiji", "Finland", "France",
			"French Guiana", "French Polynesia", "French Southern Territories",
			"Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece",
			"Greenland", "Grenada", "Guadaloupe", "Guam", "Guatamala",
			"Guinea-Bissau", "Guinea", "Guyana", "Haiti",
			"Heard and McDonald Islands", "Honduras", "Hong Kong", "Hungary",
			"Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland",
			"Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan",
			"Kazakhstan", "Kenya", "Kiribati", "Korea (North)",
			"Korea (South)", "Kuwait", "Kyrgyzstan", "Laos", "Latvia",
			"Lebanon", "Lesotho", "Liberia", "Liechtenstein", "Lithuania",
			"Luxembourg", "Macau", "Macedonia", "Madagascar", "Malasia",
			"Malawi", "Maldives", "Mali", "Malta", "Marshal Islands",
			"Martinique", "Mauritania", "Maurritius", "Mayotte", "Mexico",
			"Micronesia", "Moldova", "Monaco", "Mongolia", "Montserrat",
			"Morocco", "Mozambique", "Mynamar", "Namibia", "Nauru", "Nepal",
			"Netherland Antilles", "Netherlands", "New Caledonia",
			"New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue",
			"Norfolk Island", "Northern Marianas Islands", "Norway", "Oman",
			"Pakistan", "Palau", "Panama", "Papua New Guinea", "Paraguay",
			"Peru", "Philippines", "Pitcairn", "Poland", "Portugal",
			"Puerto Rico", "Qatar", "Reunion", "Romania", "Russian Federation",
			"Rwanda", "Saint Helena", "Saint Kitts and Nevis", "Saint Lucia",
			"Saint Pierre and Miquelon", "Saint Vincent and the Grenadines",
			"Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia",
			"Senegal", "Seychelles", "Sierra Leone", "Singapore",
			"Slovak Republic", "Slovenia", "Solomon Islands", "Somalia",
			"South Africa", "South Georgia/South Sandwich Islands", "Spain",
			"Sri Lanka", "Sudan", "Suriname", "Svalbard and Jan Mayen Islands",
			"Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan",
			"Tajikistan", "Tanzania", "Thailand", "Togo", "Tokelau", "Tonga",
			"Trinidad and Tobego", "Tunisia", "Turkey", "Turkmenistan",
			"Turks and Caicos Islands", "Tuvalu", "Uganda", "Ukraine",
			"United Arab Emirates", "United Kingdom", "Uruguay", "Uzbekistan",
			"Vanuatu", "Vatican City", "Venezuela", "Vietnam",
			"Virgin Islands (British)", "Virgin Islands (US)",
			"Wallis and Futuna Islands", "Western Sahara", "Yemen",
			"Yugoslavia", "Zaire", "Zambia", "Zimbabwe" };  
	    
	  public RegisterServerBean(){
		  this.email = System.getProperty("webmaster","");
	  }

	  
	   public String  registerServer() {
		   
		   String os = System.getProperty("os.name") + ", "+ System.getProperty("os.version");
		   String genepatternVersion = System.getProperty("GenePatternVersion");
		      
		   HttpClient client = new HttpClient();
		   PostMethod httppost = new PostMethod(action);
		  
		   httppost.addParameter("component","Server");
		   httppost.addParameter("version",genepatternVersion);
		   httppost.addParameter("build",System.getProperty("build.tag"));
		   httppost.addParameter("os", os);
			
		   httppost.addParameter("name",this.name);
		   if (title != null) httppost.addParameter("title",this.title);
		   httppost.addParameter("email",this.email);
		   httppost.addParameter("organization",this.organization);
		   httppost.addParameter("department",this.department);
		   httppost.addParameter("address1",this.address1);
		   if (address2 != null) httppost.addParameter("address2",this.address2);
		   httppost.addParameter("city",this.city);
		   httppost.addParameter("state",this.state);
		   if (zipCode != null) httppost.addParameter("zip",this.zipCode);
		   httppost.addParameter("country",this.country);
		   httppost.addParameter("join", ""+this.joinMailingList);
		   httppost.addParameter("os", os);
		   
		   // let them go on in if there was an exception but don't save 
		   // the registration to the DB.  They will be asked to register again
		   // after each restart
		   try {
			   int responseCode = client.executeMethod(httppost);
			   
			   if (responseCode >= 400) throw new HttpException();
			   saveIsRegistered();
			   UIBeanHelper.login(this.email, false, false, UIBeanHelper.getRequest(), UIBeanHelper.getResponse());
			   error = false;			   
			   return "installFrame";
		   } catch (HttpException e) {
			   System.setProperty(GPConstants.REGISTERED_SERVER, "unregistered");
			   e.printStackTrace();
			   error = true;
			   return "unregisteredServer";
		   } catch (IOException e) {
			   // TODO Auto-generated catch block
			   System.setProperty(GPConstants.REGISTERED_SERVER, "unregistered");
			   e.printStackTrace();
			   error = true;
			   return "unregisteredServer";
		   }
		 
	   }
	   public String cancelRegistration() {
		   System.setProperty(GPConstants.REGISTERED_SERVER, "unregistered");
		   String os = System.getProperty("os.name") + ", "+ System.getProperty("os.version");
		   String genepatternVersion = System.getProperty("GenePatternVersion");
	        
		   HttpClient client = new HttpClient();
		   PostMethod httppost = new PostMethod(action);
		  
		   httppost.addParameter("name","Anonymous");
		   httppost.addParameter("component","Server");
		   httppost.addParameter("version",genepatternVersion);
		   httppost.addParameter("build",System.getProperty("build.tag"));
		   httppost.addParameter("os", os);
			   
		   httppost.addParameter("email","");
		   httppost.addParameter("component","Server");
		   httppost.addParameter("organization","");
		   httppost.addParameter("department","");
		   httppost.addParameter("address1","");
		   httppost.addParameter("city","");
		   httppost.addParameter("state","");
		   httppost.addParameter("country","");
		   httppost.addParameter("join", "false");
		   
		   // let them go on in if there was an exception but don't save 
		   // the registration to the DB.  They will be asked to register again
		   // after each restart
		   try {
			   int responseCode = client.executeMethod(httppost);
			   
			   if (responseCode >= 400) throw new HttpException();
			   // we don't know them, but their download is recorded so mark the server as registered
			   saveIsRegistered();
			   
		   } catch (Exception e){
			   // swallow it and return
			   // didn't get a record back at the mother ship from the post so
			   // make the registration only good until restart
		   }
		   
		   
		   return "unregisteredServer";
	   }
	  
public static boolean isRegisteredOrDeclined(){
	
	if (System.getProperty(GPConstants.REGISTERED_SERVER, null) != null) return true;    
	else return isRegistered();
	
}
	   
	   
	   public static boolean isRegistered() {
	        log.debug("checking registration");
	        boolean upToDate = false;
	        String dbRegisteredVersion;
	        
	        String genepatternVersion = System.getProperty("GenePatternVersion");
	        
	        // check the DB
	         
	        String sql = "select value from props where key='registeredVersion"+genepatternVersion+"'";

	        try {
	            BaseDAO dao = new BaseDAO();
	            ResultSet resultSet = dao.executeSQL(sql, false);
	            if (resultSet.next()) {
	            	dbRegisteredVersion = resultSet.getString(1);
	                upToDate = (genepatternVersion.compareTo(dbRegisteredVersion) <= 0);
	            }  else {
	                upToDate = false;
	            }
	        } catch (Exception e) {
	        	//
	        }
	        return upToDate;
	    }
	   
	   private static void saveIsRegistered() {
	        log.debug("saving registration");
	        String dbRegisteredVersion;
	        
	        String genepatternVersion = System.getProperty("GenePatternVersion");
	        dbRegisteredVersion = System.getProperty(GPConstants.REGISTERED_SERVER, null);
	       
	        if (dbRegisteredVersion != null) return;
	       
	        // check the DB
	        String sql1 = "select value from props where key='registeredVersion"+genepatternVersion+"'";

	        String sqlIns = "insert into props values ('registeredVersion"+genepatternVersion+"', '"+genepatternVersion+"')";
	        

	        try {
	            BaseDAO dao = new BaseDAO();
	            dao.executeUpdate(sqlIns);
	            System.setProperty(GPConstants.REGISTERED_SERVER, genepatternVersion);
	        } catch (Exception e) {
	        	// either DB is down or we already have it there
	        	log.debug(e);
	        }
	   }

	   
	   
	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public String getOrganization() {
		return organization;
	}


	public void setOrganization(String organization) {
		this.organization = organization;
	}


	public String getDepartment() {
		return department;
	}


	public void setDepartment(String department) {
		this.department = department;
	}


	public String getAddress1() {
		return address1;
	}


	public void setAddress1(String address1) {
		this.address1 = address1;
	}


	public String getAddress2() {
		return address2;
	}


	public void setAddress2(String address2) {
		this.address2 = address2;
	}


	public String getCity() {
		return city;
	}


	public void setCity(String city) {
		this.city = city;
	}


	public String getState() {
		return state;
	}


	public void setState(String state) {
		this.state = state;
	}


	public String getZipCode() {
		return zipCode;
	}


	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}


	public String getCountry() {
		return country;
	}


	public void setCountry(String country) {
		System.out.println("SetCountry = " + country);
		this.country = country;
	}


	public boolean isAcceptLicense() {
		return acceptLicense;
	}


	public void setAcceptLicense(boolean acceptLicense) {
		this.acceptLicense = acceptLicense;
	}


	public String getOs() {
		return os;
	}


	public void setOs(String os) {
		this.os = os;
	}


	public boolean isJoinMailingList() {
		return joinMailingList;
	}


	public void setJoinMailingList(boolean joinMailingList) {
		this.joinMailingList = joinMailingList;
	}
	  
	List<SelectItem> items;
	public List<SelectItem> getCountries(){
		if (items == null){
			items = new ArrayList<SelectItem>();
			for (String c: countries){
				items.add(new SelectItem(c));		
			}
		}
		return items;
	}
	
	public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
	    if (!(email.contains("@"))) {
	        String message = "Please enter a valid emailS.";
	        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
	        ((UIInput) component).setValid(false);
	        throw new ValidatorException(facesMessage);
	    }
	}


	public boolean isError() {
		return error;
	}


	public void setError(boolean error) {
		this.error = error;
	}
	
	
	
	
}

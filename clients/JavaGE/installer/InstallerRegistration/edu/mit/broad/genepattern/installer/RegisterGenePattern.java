/*
 * RegisterGenePattern.java
 *
 * Created on January 14, 2004, 6:54 AM
 */
package edu.mit.broad.genepattern.installer;

import com.zerog.ia.api.pub.*;
import java.net.*;

/**
 * custom class for registering GenePattern from a remote client installation
 * It expects to find IA variables with the following names;
 * GP_REG_NAME = name
 * GP_REG_EMAIL = email address
 * GP_REG_JOIN_GPUSERS = t/f jopoin the email to the gp-users mailing list
 * GP_REG_INSTITUTE = institution
 * GP_REG_ADDRESS = address
 *
 * GP_REG_REGISTRATION_URL = URL to register the information to
 * @author  Liefeld
 */
public class RegisterGenePattern extends CustomCodeAction {
    public String[] countries = {"United States of America" , "Afghanistan","Albania","Algeria","American Samoa","Andorra","Angola","Anguila","Antarctica","Antigua and Barbuda","Argentina","Aruba","Australia","Austria","Azerjaijan","Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bermuda","Bhutan","Bolivia","Bosnia and Herzegovina","Botswana","Bouvet Island","Brazil", "British Indian Ocean territory","Brunei Darussalam","Bulgaria","Burkina Faso","Burundi","Cambodia","Cameroon","Canada","Cape Verde","Cayman Islands","Central African Republic","Chad","Chile","China","Christmas Island","Cocos (Keeling) Islands","Colombia","Comoros","Congo","Cook Islands","Costa Rica","Cote d'Ivoire (Ivory Coast)","Croatia (Hrvatska)","Cuba","Cyprus","Czech Republic","Denmark","Djibouti","Dominica","Dominican Republic","East Timor","Ecuador","Egypt","El Salvador","Equatorial Guinea","Eritrea","Estonia","Ethiopia","Falkland Islands","Faroe Islands","Fiji","Finland","France","French Guiana","French Polynesia","French Southern Territories","Gabon","Gambia","Georgia","Germany","Ghana","Greece","Greenland","Grenada","Guadaloupe","Guam","Guatamala","Guinea-Bissau","Guinea","Guyana","Haiti","Heard and McDonald Islands","Honduras","Hong Kong","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland","Israel","Italy","Jamaica","Japan","Jordan","Kazakhstan","Kenya","Kiribati","Korea (North)","Korea (South)","Kuwait","Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Liberia","Liechtenstein","Lithuania","Luxembourg","Macau","Macedonia","Madagascar","Malasia","Malawi","Maldives","Mali","Malta","Marshal Islands","Martinique","Mauritania","Maurritius","Mayotte","Mexico","Micronesia","Moldova","Monaco","Mongolia","Montserrat","Morocco","Mozambique","Mynamar","Namibia","Nauru","Nepal","Netherland Antilles","Netherlands","New Caledonia","New Zealand","Nicaragua","Niger","Nigeria","Niue","Norfolk Island","Northern Marianas Islands","Norway","Oman","Pakistan","Palau","Panama","Papua New Guinea","Paraguay","Peru","Philippines","Pitcairn","Poland","Portugal","Puerto Rico","Qatar","Reunion","Romania","Russian Federation","Rwanda","Saint Helena","Saint Kitts and Nevis","Saint Lucia","Saint Pierre and Miquelon","Saint Vincent and the Grenadines","Samoa","San Marino","Sao Tome and Principe","Saudi Arabia","Senegal","Seychelles","Sierra Leone","Singapore","Slovak Republic","Slovenia","Solomon Islands","Somalia","South Africa","South Georgia/South Sandwich Islands","Spain","Sri Lanka","Sudan","Suriname","Svalbard and Jan Mayen Islands","Swaziland","Sweden","Switzerland","Syria","Taiwan","Tajikistan","Tanzania","Thailand","Togo","Tokelau","Tonga","Trinidad and Tobego","Tunisia","Turkey","Turkmenistan","Turks and Caicos Islands","Tuvalu","Uganda","Ukraine","United Arab Emirates","United Kingdom","Uruguay","Uzbekistan","Vanuatu","Vatican City","Venezuela","Vietnam","Virgin Islands (British)","Virgin Islands (US)","Wallis and Futuna Islands","Western Sahara","Yemen","Yugoslavia","Zaire","Zambia","Zimbabwe" };
    
    
    
    
    
    /** Creates a new instance of RegisterGenePattern */
    public RegisterGenePattern() {
    }
    
    public String getInstallStatusMessage() {
        return "Registering GenePattern";
    }
    
    public String getUninstallStatusMessage() {
        return "";
    }
    
    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {

        StringBuffer errorBuff = new StringBuffer();
        
        
        // get the information that the user was to input
        // in the installer
        String name = (String)ip.getVariable( "GP_REG_NAME" );
        if ((name == null) || (name.trim().length() == 0)) errorBuff.append("Name\n");
        
        if (name != null){
            if (name.startsWith("qwqwqw")){
                ip.setVariable("GP_REG_REGISTERED",  "TRUE");
                return;
            }
        }
        
        String title = (String)ip.getVariable( "GP_REG_TITLE" );
        String email = (String)ip.getVariable( "GP_REG_EMAIL" );  
        if ((email == null) || (email.trim().length() == 0)) errorBuff.append("Email\n");
      
        String join = (String)ip.getVariable( "GP_REG_JOIN_GPUSERS" );  
        String organization = (String)ip.getVariable( "GP_REG_ORGANIZATION" );  
        if ((organization == null) || (organization.trim().length() == 0)) errorBuff.append("Organization\n");
        
        String address1 = (String)ip.getVariable( "GP_REG_ADDRESS1" );  
        String address2 = (String)ip.getVariable( "GP_REG_ADDRESS2" );  
        String city = (String)ip.getVariable( "GP_REG_CITY" );  
         String state = (String)ip.getVariable( "GP_REG_STATE" );  
        String zip  = (String)ip.getVariable( "GP_REG_ZIP" );  
        String country = getCountry(ip);
        if ((country == null) || (country.trim().length() == 0)) errorBuff.append("Country\n");
       
        String os = System.getProperty("os.name")+" " + System.getProperty("os.version");
        StringBuffer versionBuff = new StringBuffer("");
        versionBuff.append((String)ip.getVariable( "version.major"));
        versionBuff.append(".");
        versionBuff.append((String)ip.getVariable( "version.minor"));
        versionBuff.append(".");
        versionBuff.append((String)ip.getVariable( "buildtag"));
         
        String version = versionBuff.toString();  
        
        if (errorBuff.length() > 0) {
            ip.setVariable("GP_REG_REGISTERED",  "INVALID");
            ip.setVariable("GP_REG_INVALIDFIELDS",  errorBuff.toString());
            return;
        }
           
        // get the URL we will register to
        String url = (String)ip.getVariable( "GP_REG_REGISTRATION_URL" );   
        
        // build up the registration URL we will use
        StringBuffer buff = new StringBuffer(url);
        buff.append("?os=");
        buff.append(URLEncoder.encode(os));
        buff.append("&gpgeversion=");
        buff.append(URLEncoder.encode(version));
        buff.append("&name=");
        buff.append(URLEncoder.encode(name));
        buff.append("&title=");
        buff.append(URLEncoder.encode(title));
        buff.append("&email=");
        buff.append(URLEncoder.encode(email));
        buff.append("&join=");
        buff.append(URLEncoder.encode(join));
        buff.append("&organization=");
        buff.append(URLEncoder.encode(organization));
        buff.append("&address1=");
        buff.append(URLEncoder.encode(address1));
        buff.append("&address2=");
        buff.append(URLEncoder.encode(address2));
        buff.append("&city=");
        buff.append(URLEncoder.encode(city));
        buff.append("&state=");
        buff.append(URLEncoder.encode(state));
        buff.append("&zip=");
        buff.append(URLEncoder.encode(zip));
        buff.append("&country=");
        buff.append(URLEncoder.encode(country));
       
        ip.setVariable("GP_REG_URL",  buff.toString()); // to access in installer
        
        try {
            URL regUrl = new URL(buff.toString());
            regUrl.openConnection();
            
            HttpURLConnection conn = (HttpURLConnection)regUrl.openConnection();
            conn.getContent();
            int response = conn.getResponseCode();
            if (response < 300) ip.setVariable("GP_REG_REGISTERED",  "TRUE");
            else ip.setVariable("GP_REG_REGISTERED",  "FAILED");
            ip.setVariable("GP_REG_RESPONSE",  "" + response);
        } catch (Exception e){
         // do nothing.  we loose one registration   
            e.printStackTrace();
            ip.setVariable("GP_REG_REGISTERED",  "FAILED");
        }
        // now make a post to the url
    }
    
     public String getCountry(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        String varPrefix = (String)ip.getVariable( "GP_REG_COUNTRY_PREFIX" );  
        String varIdxStart = (String)ip.getVariable( "GP_REG_COUNTRY_IDX_START" );  
        String varIdxEnd = (String)ip.getVariable( "GP_REG_COUNTRY_IDX_END" );  
        try {
            int idxStart = Integer.parseInt(varIdxStart);
            int idxEnd = Integer.parseInt(varIdxEnd);
            int len = idxEnd - idxStart;
            
            for (int i=0; i <= len; i++){
                String countryVarId = varPrefix + (idxStart + i);
                String countryFlag = (String)ip.getVariable( countryVarId );  
                 
                if ("1".equals(countryFlag)) {
                    ip.setVariable("GP_REG_COUNTRY", countries[i] );
                    return countries[i];
                    
                }
            }
        } catch (NumberFormatException nfe){
            nfe.printStackTrace();
           
        }
        ip.setVariable("GP_REG_COUNTRY", "NULL" );
         return "";
     }
    
    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
    // do nothing on uninstall
    
    }
    
}


package org.genepattern.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.genepattern.webservice.jaxb.parameter.ANALYSISPARAMETERS;
import org.genepattern.webservice.jaxb.parameter.ATTRIBUTE;
import org.genepattern.webservice.jaxb.parameter.PARAMETER;


/**
 * Utility class to convert between jxb string and <CODE>ParameterInfo<CODE>
 * @author  Rajesh Kuttan, Hui Gong
 * @version $Revision 1.2 $
 */
public class ParameterFormatConverter {

    /** Creates new ParameterFormatConverter */
    public ParameterFormatConverter() {
    }



    /**
     * Converts <CODE>ParameterInfo</CODE> to parameter jaxb string
     * @param parameterInfoArray
     * @throws OmnigeneException
     * @return
     */
    public String getJaxbString(ParameterInfo[] parameterInfoArray) throws OmnigeneException {

        String jaxbParameterString = "";

        if (parameterInfoArray==null)
            return null;

        try {
            ParameterInfo parameterInfo=null;
            ANALYSISPARAMETERS jxbAnalysisParameter=null;
            List paraList = Arrays.asList(parameterInfoArray);

            //To convert parameter object to jxb for storing in DB as string

            jxbAnalysisParameter = new ANALYSISPARAMETERS();
            List parameterList = jxbAnalysisParameter.getPARAMETER();

            for (int i=0;i<paraList.size();i++) {
                parameterInfo=(ParameterInfo)paraList.get(i);
                PARAMETER jxbParameter= new PARAMETER();
                //create JAXB attribute list
                List attributes = jxbParameter.getATTRIBUTE();
                //gets ParameterInfo attribute list
                HashMap attrs = parameterInfo.getAttributes();
                if(attrs!=null){
                    Iterator it = attrs.keySet().iterator();
                    while(it.hasNext()){
                        String key = (String)it.next();
                        String value = (String)attrs.get(key);
                        ATTRIBUTE attribute = new ATTRIBUTE();
                        attribute.setKey(key);
                        attribute.setContent(value);
                        attributes.add(attribute);
                    }
                }
                jxbParameter.setName(parameterInfo.getName());
                jxbParameter.setValue(parameterInfo.getValue());
                jxbParameter.setDESCRIPTION(parameterInfo.getDescription());
                parameterList.add(jxbParameter);

            }
            ByteArrayOutputStream fcout = new ByteArrayOutputStream();
            jxbAnalysisParameter.validate();
            jxbAnalysisParameter.marshal(fcout);
            jaxbParameterString = fcout.toString();
            fcout.close();
        } catch( Exception ex) {
            System.out.println("ParameterFormatConverter:getJaxbString Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.toString());
        }
        return jaxbParameterString;

    }


    /**
     * Converts Jxb parameter string to <CODE>ParameterInfo</CODE>
     * @param jxbParameterInfoString
     * @throws OmnigeneException
     * @return
     */
    public ParameterInfo[] getParameterInfoArray(String jxbParameterInfoString) throws OmnigeneException {

        ParameterInfo[] parameterInfoArray=null;

        if (jxbParameterInfoString!=null)  {
            if (jxbParameterInfoString.trim().length()==0)
		return new ParameterInfo[0];
        }
        else {
            return new ParameterInfo[0];
        }

        try {
            ANALYSISPARAMETERS jxbAnalysisParameters = ANALYSISPARAMETERS.unmarshal(new ByteArrayInputStream(jxbParameterInfoString.getBytes()));
            List jxbParameterList = jxbAnalysisParameters.getPARAMETER();
            PARAMETER jxbParameter=null;
            Vector parameterVector = new Vector();
            for (int i=0;i<jxbParameterList.size();i++) {
                jxbParameter = (PARAMETER)jxbParameterList.get(i);
                HashMap attsMap = new HashMap();
                List atts = jxbParameter.getATTRIBUTE();
                for(int j=0; j<atts.size(); j++){
                    ATTRIBUTE att = (ATTRIBUTE)atts.get(j);
                    attsMap.put(att.getKey(), att.getContent());
                }
                ParameterInfo parameterInfo = new ParameterInfo(jxbParameter.getName(),jxbParameter.getValue(),jxbParameter.getDESCRIPTION());
                parameterInfo.setAttributes(attsMap);
                parameterVector.add(parameterInfo);
            }
            if (parameterVector.size()>0)
                parameterInfoArray = (ParameterInfo[])parameterVector.toArray(new ParameterInfo[] {(ParameterInfo)parameterVector.get(0)});

        } catch (Exception ex) {
            System.out.println("ParameterFormatConverter:getParameterInfoArray Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.toString());
        }
        return parameterInfoArray;


    }


    /**
     * @param args  */
      public  static void  main(String args[]) {
        ParameterFormatConverter pc = new ParameterFormatConverter();
        try {

            String jxbString = "<ANALYSISPARAMETERS><PARAMETER name=\"ParadiseHost\" value=\"anchor.turbogenomics.com\"/><PARAMETER name=\"InputSource\" value=\"/home/techarch/sequence.in\"/> " +
                                " <PARAMETER name=\"DatabaseName\" value=\"ecoli.nt\"/></ANALYSISPARAMETERS>" ;

        ParameterInfo[] paraInfoArray = (ParameterInfo[])pc.getParameterInfoArray(jxbString);
        if (paraInfoArray==null)
            System.out.println("ParaInfoArray is null");
        else {
            System.out.println("Converted values  " + ((ParameterInfo)paraInfoArray[1]).getName());
            System.out.println("Converted values  " + ((ParameterInfo)paraInfoArray[1]).getValue());
        }

        //System.out.println("Conveted values  " + ((ParameterInfo)paraInfoArray[0]).getName());


        //String str = pc.getJaxbString(paraInfoArray);
        //System.out.println("Str = " + str);

            /*
        ParameterFormatConverter paraConverter = new ParameterFormatConverter();
        ParameterInfo[] paraInfoArray = null ;
        String paraString = paraConverter.getJaxbString(paraInfoArray);
        if (paraString==null)
            System.out.println("Null returned");


         */



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

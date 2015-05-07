/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.jaxb.parameter.ANALYSISPARAMETERS;
import org.genepattern.webservice.jaxb.parameter.ATTRIBUTE;
import org.genepattern.webservice.jaxb.parameter.PARAMETER;

/**
 * Utility class to convert between jxb string and <CODE>ParameterInfo <CODE>
 *
 * @author Rajesh Kuttan, Hui Gong
 * @version $Revision 1.2 $
 */
public class ParameterFormatConverter {
    private static Logger log = Logger.getLogger(ParameterFormatConverter.class);

    //use singleton instance of JAXBContext to avoid memory leak, GP-2868
    private static JAXBContext analysisParametersContext = null;
    static {
        try {
            analysisParametersContext = JAXBContext.newInstance(ANALYSISPARAMETERS.class);
        }
        catch (JAXBException e) {
            log.error("Configuration error in ParameterFormatConverter, JAXBException thrown: " + e.getLocalizedMessage(), e);
        }
    }

    /** Creates new ParameterFormatConverter */
    private ParameterFormatConverter() {
    }

    /**
     * Converts <CODE>ParameterInfo</CODE> to parameter jaxb string for storing in DB.
     *
     * @param parameterInfoArray
     * @throws OmnigeneException
     * @return The string.
     */
    public static String getJaxbString(ParameterInfo[] parameterInfoArray) throws OmnigeneException {
        if (parameterInfoArray == null) {
            return null;
        }

        //marshaller is not thread-safe so must use on instance per method invocation
        Marshaller marshaller = null;
        try {
            marshaller = analysisParametersContext.createMarshaller();
        }
        catch (JAXBException e) {
            log.error("getJaxbString error in createMarshaller: "+e.getLocalizedMessage(), e);
            return null;
        }
        
        String jaxbParameterString = "";
        try { 
            ANALYSISPARAMETERS jxbAnalysisParameter = new ANALYSISPARAMETERS();
            List<PARAMETER> parameterList = jxbAnalysisParameter.getPARAMETER();

            for (ParameterInfo parameterInfo : parameterInfoArray) {
                PARAMETER jxbParameter = new PARAMETER();
                // create JAXB attribute list
                List<ATTRIBUTE> attributes = jxbParameter.getATTRIBUTE();
                // gets ParameterInfo attribute list
                HashMap attrs = parameterInfo.getAttributes();
                if (attrs != null) {
                    Iterator it = attrs.keySet().iterator();
                    while (it.hasNext()) {
                        String key = (String) it.next();
                        String value = (String) attrs.get(key);
                        ATTRIBUTE attribute = new ATTRIBUTE();
                        attribute.setKey(key);
                        attribute.setContent(value != null ? URLEncoder.encode(value, GPConstants.UTF8) : null);
                        attributes.add(attribute);
                    }
                }
                jxbParameter.setName(parameterInfo.getName());
                /*
                 * BUG 55: Due to what appears to be a bug in JAXB 1.0 early
                 * access, the unmarshalling of arguments involves the trimming
                 * to a single space of any multiple space character sequence.
                 * As a workaround, parameters are being URLEncoded, thus
                 * turning spaces into pluses. The reverse operation is
                 * performed when they are unmarshalled.
                 */
                String val = parameterInfo.getValue();
                if (val == null) {
                    val = "";
                }
                jxbParameter.setValue(URLEncoder.encode(val, GPConstants.UTF8));
                jxbParameter.setDESCRIPTION(parameterInfo.getDescription());
                parameterList.add(jxbParameter);
            }
            ByteArrayOutputStream fcout = new ByteArrayOutputStream();

            // jln
            marshaller.marshal(jxbAnalysisParameter, fcout);
            jaxbParameterString = fcout.toString();
            fcout.close();
        } 
        catch (Exception ex) {
            log.error("ParameterFormatConverter:getJaxbString Error " + ex.getMessage(), ex);
            throw new OmnigeneException(ex);
        }
        return jaxbParameterString;
    }

    /**
     * Converts Jxb parameter string to a <CODE>ParameterInfo</CODE> array.
     *
     * @param jxbParameterInfoString
     * @throws OmnigeneException
     * @return The parameter array.
     */
    public static ParameterInfo[] getParameterInfoArray(String jxbParameterInfoString) throws OmnigeneException {
        ParameterInfo[] parameterInfoArray = new ParameterInfo[0];
        if (jxbParameterInfoString == null || jxbParameterInfoString.trim().length() == 0) {
            return parameterInfoArray;
        }
        
        Unmarshaller unmarshaller = null;
        try {
            unmarshaller = analysisParametersContext.createUnmarshaller();
        }
        catch (JAXBException e) {
            log.error("getJaxbString error in createUnmarshaller: "+e.getLocalizedMessage(), e);
            return parameterInfoArray;
        }

        try { 
        	ANALYSISPARAMETERS jxbAnalysisParameters = (ANALYSISPARAMETERS) unmarshaller.unmarshal(new ByteArrayInputStream(jxbParameterInfoString.getBytes()));
            List<PARAMETER> jxbParameterList = jxbAnalysisParameters.getPARAMETER();
            PARAMETER jxbParameter = null;
            List<ParameterInfo> parameterVector = new ArrayList<ParameterInfo>();
            for (int i = 0; i < jxbParameterList.size(); i++) {
                jxbParameter = (PARAMETER) jxbParameterList.get(i);
                HashMap attsMap = new HashMap();
                for (ATTRIBUTE att : jxbParameter.getATTRIBUTE()) {
                    String content = att.getContent();
                    attsMap.put(att.getKey(), content != null ? URLDecoder.decode(content, "UTF-8") : null);
                }

                /*
                 * BUG 55: Due to what appears to be a bug in JAXB 1.0 early
                 * access, the unmarshalling of arguments involves the trimming
                 * to a single space of any multiple space character sequence.
                 * As a workaround, parameters are being URLEncoded, thus
                 * turning spaces into pluses. The reverse operation is
                 * performed when they are unmarshalled.
                 */

                ParameterInfo parameterInfo = new ParameterInfo(jxbParameter.getName(), URLDecoder.decode(jxbParameter
                        .getValue(), GPConstants.UTF8), jxbParameter.getDESCRIPTION());
                parameterInfo.setAttributes(attsMap);
                parameterVector.add(parameterInfo);
            }
            if (parameterVector.size() > 0)
                parameterInfoArray = (ParameterInfo[]) parameterVector
                        .toArray(new ParameterInfo[] { (ParameterInfo) parameterVector.get(0) });

        } 
        catch (Exception ex) {
            log.error("ParameterFormatConverter:getParameterInfoArray Error " + ex.getMessage(), ex);
            throw new OmnigeneException(ex.toString());
        }
        return parameterInfoArray;
    }

    private static ParameterInfo[] stripPasswords(ParameterInfo[] params) {
        for (int i = 0; i < params.length; i++) {
            ParameterInfo p = params[i];
            if (p.isPassword()) {
                p.setValue("");
            }
        }
        return params;
    }

    public static String stripPasswords(String jxbParameterInfoString) {
        ParameterInfo[] params = getParameterInfoArray(jxbParameterInfoString);
        return getJaxbString(stripPasswords(params));
    }

//    /**
//     * @param args
//     */
//    public static void main(String args[]) {
//        ParameterFormatConverter pc = new ParameterFormatConverter();
//        try {
//
//            String jxbString = "<ANALYSISPARAMETERS><PARAMETER name=\"ParadiseHost\" value=\"anchor.turbogenomics.com\"/><PARAMETER name=\"InputSource\" value=\"/home/techarch/sequence.in\"/> "
//                    + " <PARAMETER name=\"DatabaseName\" value=\"ecoli.nt\"/></ANALYSISPARAMETERS>";
//
//            ParameterInfo[] paraInfoArray = (ParameterInfo[]) pc.getParameterInfoArray(jxbString);
//            if (paraInfoArray == null)
//                System.out.println("ParaInfoArray is null");
//            else {
//                System.out.println("Converted values  " + ((ParameterInfo) paraInfoArray[1]).getName());
//                System.out.println("Converted values  " + ((ParameterInfo) paraInfoArray[1]).getValue());
//            }
//
//            // System.out.println("Conveted values " +
//            // ((ParameterInfo)paraInfoArray[0]).getName());
//
//            // String str = pc.getJaxbString(paraInfoArray);
//            // System.out.println("Str = " + str);
//
//            /*
//             * ParameterFormatConverter paraConverter = new
//             * ParameterFormatConverter(); ParameterInfo[] paraInfoArray = null ;
//             * String paraString = paraConverter.getJaxbString(paraInfoArray);
//             * if (paraString==null) System.out.println("Null returned");
//             *
//             *
//             */
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}

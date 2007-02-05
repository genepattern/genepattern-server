/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskExecutor;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * A utility class used to read user input from the console and to create array
 * of parameters for running jobs.
 * 
 * @author Joshua Gould
 */
public class Util {
    private static java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

    private Util() {
    }

    /**
     * Prompts the user for input from the console.
     * 
     * @param prompt
     *            The user prompt.
     * @return The user entered text.
     * @exception java.io.IOException
     *                If an error occurs while reading the input from the
     *                console.
     */
    public static String prompt(String prompt) throws java.io.IOException {
        System.out.print(prompt);
        return br.readLine();
    }

    public static ParameterInfo[] createParameterInfoArray(TaskInfo taskInfo, Parameter[] parameters)
            throws WebServiceException {

        ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
        List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();

        Map<String, ParameterInfo> paramName2FormalParam = new HashMap<String, ParameterInfo>();
        if (formalParameters != null) {
            for (int i = 0, length = formalParameters.length; i < length; i++) {
                paramName2FormalParam.put(formalParameters[i].getName(), formalParameters[i]);
            }
        }

        if (parameters != null) {
            for (int i = 0, length = parameters.length; i < length; i++) {
                ParameterInfo formalParam = paramName2FormalParam.remove(parameters[i].getName());
                if (formalParam == null) {
                    if (parameters[i].getName().equals(GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM)) {
                        formalParam = new ParameterInfo(parameters[i].getName(), parameters[i].getValue(), "");
                    } else {
                        throw new WebServiceException("Unknown parameter: " + parameters[i].getName());
                    }
                }
                Map formalAttributes = formalParam.getAttributes();
                if (formalAttributes == null) {
                    formalAttributes = new HashMap();
                }
                String value = parameters[i].getValue();

                if (value == null) {
                    value = (String) formalAttributes.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
                }
                if (value == null && !isOptional(formalParam)) {
                    throw new WebServiceException("Missing value for required parameter "
                            + formalParameters[i].getName());

                }
                value = sub(formalParam, value);
                ParameterInfo p = new ParameterInfo(formalParam.getName(), value, "");
                setAttributes(formalParam, p);
                actualParameters.add(p);
            }
        }

        // go through parameters that were not provided by user
        for (Iterator<String> it = paramName2FormalParam.keySet().iterator(); it.hasNext();) {
            String name = it.next();
            ParameterInfo formalParam = paramName2FormalParam.get(name);
            String value = (String) formalParam.getAttributes().get((String) TaskExecutor.PARAM_INFO_DEFAULT_VALUE[0]);
            if (value == null && !isOptional(formalParam)) {
                throw new WebServiceException("Missing value for required parameter " + formalParam.getName());

            }
            value = sub(formalParam, value);
            ParameterInfo actual = new ParameterInfo(formalParam.getName(), value, "");
            setAttributes(formalParam, actual);
            actualParameters.add(actual);

        }

        return (ParameterInfo[]) actualParameters.toArray(new ParameterInfo[0]);
    }

    private static boolean isOptional(ParameterInfo formalParameter) {
        String sOptional = (String) formalParameter.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0]);
        return (sOptional != null && sOptional.length() > 0);

    }

    private static void setAttributes(ParameterInfo formalParam, ParameterInfo actualParam) {

        if (formalParam.isInputFile()) {
            HashMap actualAttributes = new HashMap();
            actualParam.setAttributes(actualAttributes);
            String value = actualParam.getValue();
            actualAttributes.put(GPConstants.PARAM_INFO_CLIENT_FILENAME[0], value);
            if (value != null && new java.io.File(value).exists()) {
                actualParam.setAsInputFile();
            } else if (value != null) {
                actualAttributes.remove("TYPE");
                actualAttributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            }
        }
    }

    private static String sub(ParameterInfo formalParam, String value) throws WebServiceException {
        // see if parameter belongs to a set of choices, e.g. 1=T-Test.
        // If so substitute 1 for T-Test, also check to see if value is
        // valid
        String choicesString = formalParam.getValue();
        if (value != null && choicesString != null && !choicesString.equals("")) {
            String[] choices = choicesString.split(";");
            boolean validValue = false;
            for (int j = 0; j < choices.length && !validValue; j++) {
                String[] choiceValueAndChoiceUIValue = choices[j].split("=");
                if (value.equals(choiceValueAndChoiceUIValue[0])) {
                    validValue = true;
                } else if (choiceValueAndChoiceUIValue.length == 2 && value.equals(choiceValueAndChoiceUIValue[1])) {
                    value = choiceValueAndChoiceUIValue[0];
                    validValue = true;
                }
            }
            if (!validValue) {
                throw new WebServiceException("Illegal value for parameter " + formalParam.getName() + ": " + value);
            }
        }
        return value;
    }

    /**
     * Returns the model type of an odf file. The input stream is closed.
     * 
     * @param is
     *            An input stream to an odf file
     * @return the odf module type or the empty string if unable to determine
     *         the model
     */
    public static String getOdfModelType(InputStream is) {
        String model = "";
        BufferedReader inputB = null;
        try {
            inputB = new BufferedReader(new InputStreamReader(is));
            String modelLine = inputB.readLine();
            while (modelLine != null && !modelLine.startsWith("Model")) {
                modelLine = inputB.readLine();
            }

            if (modelLine != null) {
                model = modelLine.substring(modelLine.indexOf("=") + 1).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputB != null) {
                try {
                    inputB.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return model;
    }
}
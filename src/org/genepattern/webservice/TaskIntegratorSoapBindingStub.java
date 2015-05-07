/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 * TaskIntegratorSoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.genepattern.webservice;

import org.apache.xml.xml_soap.MapItem;

public class TaskIntegratorSoapBindingStub extends org.apache.axis.client.Stub {
    private java.util.Vector cachedSerClasses = new java.util.Vector();

    private java.util.Vector cachedSerQNames = new java.util.Vector();

    private java.util.Vector cachedSerFactories = new java.util.Vector();

    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc[] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[38];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
        _initOperationDesc4();
        _initOperationDesc5();
    }

    private static void _initOperationDesc1() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("clone");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "name"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "cloneReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("delete");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("install");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("errorMessage");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "message"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteTask");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("installTask");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("statusMessage");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "message"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("beginProgress");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "message"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("continueProgress");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "percentComplete"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("endProgress");
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[9] = oper;

    }

    private static void _initOperationDesc2() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDocFiles");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_apachesoap_DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getDocFilesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("exportToZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "taskName"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "recursive"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "boolean"),
                boolean.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://xml.apache.org/xml-soap", "DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "exportToZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("exportToZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "taskName"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://xml.apache.org/xml-soap", "DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "exportToZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("exportSuiteToZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://xml.apache.org/xml-soap", "DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "exportSuiteToZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("importZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "handler"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                        "DataHandler"), javax.activation.DataHandler.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "privacy"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper
                .setReturnQName(new javax.xml.namespace.QName("",
                        "importZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("importZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "handler"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                        "DataHandler"), javax.activation.DataHandler.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "privacy"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "recursive"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "boolean"),
                boolean.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper
                .setReturnQName(new javax.xml.namespace.QName("",
                        "importZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[15] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("installSuite");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[16] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("installSuite");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "suiteInfo"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("org.genepattern.webservice",
                        "SuiteInfo"), SuiteInfo.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "installSuiteReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[17] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("installSuite");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "zipFile"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "anyType"),
                java.lang.Object.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "installSuiteReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[18] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("installSuite");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "suiteInfo"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("org.genepattern.webservice",
                        "SuiteInfo"), SuiteInfo.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "supportFiles"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_apachesoap_DataHandler"),
                javax.activation.DataHandler[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "installSuiteReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[19] = oper;

    }

    private static void _initOperationDesc3() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("importZipFromURL");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "privacy"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "recursive"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "boolean"),
                boolean.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "importZipFromURLReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[20] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("importZipFromURL");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "privacy"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "importZipFromURLReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[21] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isZipOfZips");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "isZipOfZipsReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[22] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getSupportFileNames");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_soapenc_string"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getSupportFileNamesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[23] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getSupportFile");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileName"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://xml.apache.org/xml-soap", "DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getSupportFileReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[24] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getSupportFiles");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_apachesoap_DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getSupportFilesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[25] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getSupportFiles");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_apachesoap_DataHandler"));
        oper.setReturnClass(javax.activation.DataHandler[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getSupportFilesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[26] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getLastModificationTimes");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_xsd_long"));
        oper.setReturnClass(long[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getLastModificationTimesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[27] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("modifyTask");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "accessId"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "taskName"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "description"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "parameterInfoArray"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("org.genepattern.webservice",
                        "ParmInfoArray"), ParameterInfo[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "taskAttributes"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                        "Map"), java.util.HashMap.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "dataHandlers"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_apachesoap_DataHandler"),
                javax.activation.DataHandler[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "modifyTaskReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[28] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteFiles");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "deleteFilesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[29] = oper;

    }

    private static void _initOperationDesc4() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("cloneTask");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "oldLSID"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "cloneName"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper
                .setReturnQName(new javax.xml.namespace.QName("",
                        "cloneTaskReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[30] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDocFileNames");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_soapenc_string"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "getDocFileNamesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[31] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isSuiteZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "isSuiteZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[32] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isPipelineZip");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "isPipelineZipReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[33] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("filenameFromURL");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "url"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "filenameFromURLReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[34] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("modifySuite");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "access_id"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "lsid"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "name"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "description"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "author"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "owner"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"),
                java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "moduleLsids"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "dataHandlers"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_apachesoap_DataHandler"),
                javax.activation.DataHandler[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "fileNames"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "ArrayOf_soapenc_string"), java.lang.String[].class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("",
                "modifySuiteReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[35] = oper;

    }

    private static void _initOperationDesc5() {
        //getPermittedAccessId
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getPermittedAccessId");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "access_id"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://www.w3.org/2001/XMLSchema", "int"), int.class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(int.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getPermittedAccessIdReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[36] = oper;
        
        //checkPermission
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("checkPermission");
        param = new org.apache.axis.description.ParameterDesc(
                new javax.xml.namespace.QName("", "permission"),
                org.apache.axis.description.ParameterDesc.IN,
                new javax.xml.namespace.QName(
                        "http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class,
                false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "checkPermissionReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                new javax.xml.namespace.QName(
                        "http://localhost:8080/gp/services/TaskIntegrator",
                        "fault"),
                "org.genepattern.webservice.WebServiceException",
                new javax.xml.namespace.QName(
                        "http://webservice.genepattern.org",
                        "WebServiceException"), true));
        _operations[37] = oper;
    }

    public TaskIntegratorSoapBindingStub() throws org.apache.axis.AxisFault {
        this(null);
    }

    public TaskIntegratorSoapBindingStub(java.net.URL endpointURL,
            javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        this(service);
        super.cachedEndpoint = endpointURL;
    }

    public TaskIntegratorSoapBindingStub(javax.xml.rpc.Service service)
            throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service) super.service)
                .setTypeMappingVersion("1.2");
        java.lang.Class cls;
        javax.xml.namespace.QName qName;
        javax.xml.namespace.QName qName2;
        java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
        java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
        java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
        java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
        java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
        java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
        java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
        java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
        java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
        java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
        qName = new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_apachesoap_DataHandler");
        cachedSerQNames.add(qName);
        cls = javax.activation.DataHandler[].class;
        cachedSerClasses.add(cls);
        qName = new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                "DataHandler");
        qName2 = null;
        cachedSerFactories
                .add(new org.apache.axis.encoding.ser.ArraySerializerFactory(
                        qName, qName2));
        cachedDeserFactories
                .add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

        qName = new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_soapenc_string");
        cachedSerQNames.add(qName);
        cls = java.lang.String[].class;
        cachedSerClasses.add(cls);
        qName = new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/encoding/", "string");
        qName2 = null;
        cachedSerFactories
                .add(new org.apache.axis.encoding.ser.ArraySerializerFactory(
                        qName, qName2));
        cachedDeserFactories
                .add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

        qName = new javax.xml.namespace.QName(
                "http://localhost:8080/gp/services/TaskIntegrator",
                "ArrayOf_xsd_long");
        cachedSerQNames.add(qName);
        cls = long[].class;
        cachedSerClasses.add(cls);
        qName = new javax.xml.namespace.QName(
                "http://www.w3.org/2001/XMLSchema", "long");
        qName2 = null;
        cachedSerFactories
                .add(new org.apache.axis.encoding.ser.ArraySerializerFactory(
                        qName, qName2));
        cachedDeserFactories
                .add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

        qName = new javax.xml.namespace.QName(
                "http://webservice.genepattern.org", "WebServiceException");
        cachedSerQNames.add(qName);
        cls = org.genepattern.webservice.WebServiceException.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName = new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                "mapItem");
        cachedSerQNames.add(qName);
        cls = MapItem.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName = new javax.xml.namespace.QName("org.genepattern.webservice",
                "ParmInfo");
        cachedSerQNames.add(qName);
        cls = ParameterInfo.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName = new javax.xml.namespace.QName("org.genepattern.webservice",
                "ParmInfoArray");
        cachedSerQNames.add(qName);
        cls = ParameterInfo[].class;
        cachedSerClasses.add(cls);
        qName = new javax.xml.namespace.QName("org.genepattern.webservice",
                "ParmInfo");
        qName2 = null;
        cachedSerFactories
                .add(new org.apache.axis.encoding.ser.ArraySerializerFactory(
                        qName, qName2));
        cachedDeserFactories
                .add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

        qName = new javax.xml.namespace.QName("org.genepattern.webservice",
                "SuiteInfo");
        cachedSerQNames.add(qName);
        cls = SuiteInfo.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName = new javax.xml.namespace.QName("org.genepattern.webservice",
                "TaskInfoAttributes");
        cachedSerQNames.add(qName);
        cls = MapItem[].class;
        cachedSerClasses.add(cls);
        qName = new javax.xml.namespace.QName("http://xml.apache.org/xml-soap",
                "mapItem");
        qName2 = new javax.xml.namespace.QName("", "item");
        cachedSerFactories
                .add(new org.apache.axis.encoding.ser.ArraySerializerFactory(
                        qName, qName2));
        cachedDeserFactories
                .add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

    }

    protected org.apache.axis.client.Call createCall()
            throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call
                            .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
                    _call
                            .setEncodingStyle(org.apache.axis.Constants.URI_SOAP11_ENC);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses
                                .get(i);
                        javax.xml.namespace.QName qName = (javax.xml.namespace.QName) cachedSerQNames
                                .get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class) cachedSerFactories
                                    .get(i);
                            java.lang.Class df = (java.lang.Class) cachedDeserFactories
                                    .get(i);
                            _call
                                    .registerTypeMapping(cls, qName, sf, df,
                                            false);
                        } else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory) cachedSerFactories
                                    .get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory) cachedDeserFactories
                                    .get(i);
                            _call
                                    .registerTypeMapping(cls, qName, sf, df,
                                            false);
                        }
                    }
                }
            }
            return _call;
        } catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault(
                    "Failure trying to get the Call object", _t);
        }
    }

    public java.lang.String clone(java.lang.String lsid, java.lang.String name)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org", "clone"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    lsid, name });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void delete(java.lang.String lsid) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org", "delete"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void install(java.lang.String lsid) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org", "install"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void errorMessage(java.lang.String message)
            throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "errorMessage"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { message });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public void deleteTask(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "deleteTask"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void installTask(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "installTask"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void statusMessage(java.lang.String message)
            throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "statusMessage"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { message });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public void beginProgress(java.lang.String message)
            throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "beginProgress"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { message });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public void continueProgress(int percentComplete)
            throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "continueProgress"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { new java.lang.Integer(
                            percentComplete) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public void endProgress() throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "endProgress"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {});

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler[] getDocFiles(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getDocFiles"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler[]) org.apache.axis.utils.JavaUtils
                            .convert(_resp,
                                    javax.activation.DataHandler[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler exportToZip(java.lang.String taskName,
            boolean recursive) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "exportToZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    taskName, new java.lang.Boolean(recursive) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler) org.apache.axis.utils.JavaUtils
                            .convert(_resp, javax.activation.DataHandler.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler exportToZip(java.lang.String taskName)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "exportToZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { taskName });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler) org.apache.axis.utils.JavaUtils
                            .convert(_resp, javax.activation.DataHandler.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler exportSuiteToZip(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "exportSuiteToZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler) org.apache.axis.utils.JavaUtils
                            .convert(_resp, javax.activation.DataHandler.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }
    
    public int getPermittedAccessId(int access_id) throws java.rmi.RemoteException {
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[36]);
        _call.setUseSOAPAction(true);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "getPermittedAccessId"));
        setRequestHeaders(_call);
        setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{new Integer(access_id)});
        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException) _resp;
        }
        else if (_resp instanceof Integer) {
            return ((Integer)_resp).intValue();
        }
        else {
            throw new java.rmi.RemoteException("Unexpected error in SOAP call to getPermittedAccessId: Integer not returned");
        }
    }
    
    public boolean checkPermission(String permission) throws java.rmi.RemoteException {
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[37]);
        _call.setUseSOAPAction(true);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "checkPermission"));
        setRequestHeaders(_call);
        setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{permission});
        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException) _resp;
        }
        else if (_resp instanceof Boolean) {
            return ((Boolean)_resp).booleanValue();
        }
        else {
            throw new java.rmi.RemoteException("Unexpected error in SOAP call to checkPermission: Boolean not returned");
        }
    }

    public java.lang.String importZip(javax.activation.DataHandler handler,
            int privacy) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "importZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    handler, new java.lang.Integer(privacy) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String importZip(javax.activation.DataHandler handler,
            int privacy, boolean recursive) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "importZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    handler, new java.lang.Integer(privacy),
                    new java.lang.Boolean(recursive) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public void installSuite(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "installSuite"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            }
            extractAttachments(_call);
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String installSuite(SuiteInfo suiteInfo)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "installSuite"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { suiteInfo });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String installSuite(java.lang.Object zipFile)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "installSuite"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { zipFile });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String installSuite(SuiteInfo suiteInfo,
            javax.activation.DataHandler[] supportFiles,
            java.lang.String[] fileNames) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "installSuite"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    suiteInfo, supportFiles, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String importZipFromURL(java.lang.String url, int privacy,
            boolean recursive) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "importZipFromURL"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] { url,
                    new java.lang.Integer(privacy),
                    new java.lang.Boolean(recursive) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String importZipFromURL(java.lang.String url, int privacy)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[21]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "importZipFromURL"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] { url,
                    new java.lang.Integer(privacy) });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public boolean isZipOfZips(java.lang.String url)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[22]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "isZipOfZips"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { url });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return ((java.lang.Boolean) _resp).booleanValue();
                } catch (java.lang.Exception _exception) {
                    return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils
                            .convert(_resp, boolean.class)).booleanValue();
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String[] getSupportFileNames(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[23]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getSupportFileNames"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String[]) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler getSupportFile(java.lang.String lsid,
            java.lang.String fileName) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[24]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getSupportFile"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    lsid, fileName });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler) org.apache.axis.utils.JavaUtils
                            .convert(_resp, javax.activation.DataHandler.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler[] getSupportFiles(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[25]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getSupportFiles"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler[]) org.apache.axis.utils.JavaUtils
                            .convert(_resp,
                                    javax.activation.DataHandler[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public javax.activation.DataHandler[] getSupportFiles(
            java.lang.String lsid, java.lang.String[] fileNames)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[26]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getSupportFiles"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    lsid, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (javax.activation.DataHandler[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (javax.activation.DataHandler[]) org.apache.axis.utils.JavaUtils
                            .convert(_resp,
                                    javax.activation.DataHandler[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public long[] getLastModificationTimes(java.lang.String lsid,
            java.lang.String[] fileNames) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[27]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getLastModificationTimes"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    lsid, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (long[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (long[]) org.apache.axis.utils.JavaUtils.convert(
                            _resp, long[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String modifyTask(int accessId, java.lang.String taskName,
            java.lang.String description, ParameterInfo[] parameterInfoArray,
            java.util.HashMap taskAttributes,
            javax.activation.DataHandler[] dataHandlers,
            java.lang.String[] fileNames) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[28]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "modifyTask"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] {
                            new java.lang.Integer(accessId), taskName,
                            description, parameterInfoArray, taskAttributes,
                            dataHandlers, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String deleteFiles(java.lang.String lsid,
            java.lang.String[] fileNames) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[29]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "deleteFiles"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    lsid, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String cloneTask(java.lang.String oldLSID,
            java.lang.String cloneName) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[30]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "cloneTask"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    oldLSID, cloneName });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String[] getDocFileNames(java.lang.String lsid)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[31]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "getDocFileNames"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { lsid });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String[]) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String[]) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String[].class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public boolean isSuiteZip(java.lang.String url)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[32]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName(
                        "http://server.webservice.server.genepattern.org",
                        "isSuiteZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { url });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return ((java.lang.Boolean) _resp).booleanValue();
                } catch (java.lang.Exception _exception) {
                    return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils
                            .convert(_resp, boolean.class)).booleanValue();
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public boolean isPipelineZip(java.lang.String url)
            throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[33]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "isPipelineZip"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { url });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return ((java.lang.Boolean) _resp).booleanValue();
                } catch (java.lang.Exception _exception) {
                    return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils
                            .convert(_resp, boolean.class)).booleanValue();
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String filenameFromURL(java.lang.String url)
            throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[34]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "filenameFromURL"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call
                    .invoke(new java.lang.Object[] { url });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            throw axisFaultException;
        }
    }

    public java.lang.String modifySuite(int access_id, java.lang.String lsid,
            java.lang.String name, java.lang.String description,
            java.lang.String author, java.lang.String owner,
            java.lang.String[] moduleLsids,
            javax.activation.DataHandler[] dataHandlers,
            java.lang.String[] fileNames) throws java.rmi.RemoteException,
            org.genepattern.webservice.WebServiceException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[35]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName(
                "http://server.webservice.server.genepattern.org",
                "modifySuite"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp = _call.invoke(new java.lang.Object[] {
                    new java.lang.Integer(access_id), lsid, name, description,
                    author, owner, moduleLsids, dataHandlers, fileNames });

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof org.genepattern.webservice.WebServiceException) {
                    throw (org.genepattern.webservice.WebServiceException) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

}

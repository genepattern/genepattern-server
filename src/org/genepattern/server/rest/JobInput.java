package org.genepattern.server.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;

/**
 * Representation of user-supplied input parameters for a new job to be added to the GP server.
 * 
 * 
 * Example:
 *     lsid: urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.1
 *     inputFiles: [
 *         <GenePatternURL>users/admin/tutorial/all_aml_test.gct,
 *         <GenePatternURL>users/admin/tutorial/all_aml_train.gct,
 *     ]
 * @author pcarr
 *
 */
public class JobInput {
    /**
     * Unique identifier for a step in a pipeline.
     * @author pcarr
     */
    public static class StepId {
        private String id;
        
        public StepId(final String id) {
            this.id=id;
        }
        
        public String getId() {
            return id;
        }
        
        public int hashCode() {
            if (id==null) {
                return "".hashCode();
            }
            return id.hashCode();
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof StepId)) {
                return false;
            }
            if (id==null) {
                return ((StepId)obj).id==null;
            }
            return id.equals(((StepId)obj).id);
        }
    }

    public static class Param {
        public Param(ParamId id) {
            this.id=id;
        }
        public void addValue(ParamValue val) {
            if (values==null) {
                values=new ArrayList<ParamValue>();
            }
            values.add(val);
        }
        private ParamId id;
        private List<ParamValue> values;
        
        public ParamId getParamId() {
            return id;
        }
        
        public List<ParamValue> getValues() {
            return values;
        }
    }


    /**
     * Unique identifier for a parameter in a module.
     * @author pcarr
     *
     */
    public static class ParamId {
        transient int hashCode;
        private String fqName;
        public ParamId(final String fqName) {
            if (fqName==null) {
                throw new IllegalArgumentException("fqName==null");
            }
            if (fqName.length()==0) {
                throw new IllegalArgumentException("fqName is empty");
            }
            this.fqName=fqName;
            this.hashCode=fqName.hashCode();
        }
        public String getFqName() {
            return fqName;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof ParamId) {
                return fqName.equals( ((ParamId) obj).fqName );
            }
            return false;
        }
        public int hashCode() {
            return hashCode;
        }
    }
//    public static class ParamIdNew {
//        //can be empty
//        private List<StepId> parentSteps=null;
//        private String name;
//        
//        
//        public int hashCode() {
//            StringBuffer buf=new StringBuffer();
//            if (parentSteps!=null) {
//                for(StepId parentStep : parentSteps) {
//                    if (parentStep.getId()==null) {
//                        buf.append("null.");
//                    }
//                    else {
//                        buf.append( parentStep.getId() );
//                        buf.append(".");
//                    }
//                }
//            }
//            if (name==null) {
//                buf.append(" null");
//            }
//            else {
//                buf.append(name);
//            }
//            return buf.toString().hashCode();
//        }
//        
//        public boolean equals(Object obj) {
//            if (!(obj instanceof ParamId)) {
//                return false;
//            }
//            return hashCode() == obj.hashCode();
//        }
//    }

    public static class ParamValue {
        public ParamValue(String val) {
            this.value=val;
        }
        private String value;
        public String getValue() {
            return value;
        }
    }
    

//    /**
//     * The unique id for the job to be added to the server. 
//     *     In some cases this is set before POSTing, and in other cases it is not set.
//     */
//    private String jobId;
//    public String getJobId() {
//        return this.jobId;
//    }
 
    /**
     * The lsid for a module installed on the GP server.
     */
    private String lsid;
    public void setLsid(final String lsid) {
        this.lsid=lsid;
    }
    public String getLsid() {
        return this.lsid;
    }

    /**
     * The list of user-supplied parameter values.
     */
    private Map<ParamId, Param> params;
    public Map<ParamId, Param> getParams() {
        return params;
    }
    
    public void addValue(final String name, final String value) {
        ParamId id = new ParamId(name);
        Param param;
        if (params==null) {
            params=new HashMap<ParamId, Param>();
        }
        if (params.containsKey(id)){
            param=params.get(id);
        }
        else {
            param=new Param(id);
            params.put(id, param);
        }
        param.addValue(new ParamValue(value));
    }

    private GpFilePath inputFileDir;
    public GpFilePath getInputFileDir() {
        return inputFileDir;
    }
    public void setInputFileDir(final GpFilePath inputFileDir) {
        this.inputFileDir=inputFileDir;
    }
    public GpFilePath initInputFileDir(final String currentUser) throws Exception {
        Context userContext=ServerConfiguration.Context.getContextForUser(currentUser);
        return initInputFileDir(userContext);
    }
    public GpFilePath initInputFileDir(final Context userContext) throws Exception {
        //default path for adding new input files (and filelist files) for a job
        //    Note: circa GP 3.5.1, we don't have a valid job id when we call this method
        File file=new File("temp/input/"+new Date().getTime()+"/");
        GpFilePath input=GpFileObjFactory.getUserUploadFile(userContext, file);
        return input;
    }
}

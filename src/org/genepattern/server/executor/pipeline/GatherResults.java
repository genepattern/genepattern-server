/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * Utility class which generates a filelist of all of the result files submitted by a pipeline.
 * Developed as part of the prototype batch pipeline.
 * 
 * The idea is that in the pipeline designer, a user selects a module which has a filelist input parameter,
 * and for that parameter, they specify,
 *     Use filelist from <previous batch step>, optionally filter by <glob pattern>, optionally order by <order flag>.
 *     
 * Questions:
 *     1) what is the rule for getting the output files from a job? Should we consult the filesytem (e.g. 'ls') or should we
 *        use the list of output files stored in the GP database.
 *        At the moment, this implementation uses 'ls'.
 * 
 * @author pcarr
 *
 */
public class GatherResults {
    private static Logger log = Logger.getLogger(GatherResults.class);
    
    /**
     * The jobId of the completed batch step, assume it's a pipeline.
     */
    private JobInfo rootJobInfo;
    /**
     * The list of all result files, recursively generated, for all child jobs of the rootJobInfo.
     */
    private List<ParameterInfo> allResultFiles;
    
    /**
     * @param rootJobInfo
     * @param allResultFiles, the recursive, ordered list of result files, already computed in the PipelineHandler.
     */
    public GatherResults(JobInfo rootJobInfo, List<ParameterInfo> allResultFiles) {
        this.rootJobInfo = rootJobInfo;
        this.allResultFiles = allResultFiles;
    }
    
    final private Comparator<File> filenameComparator = new Comparator<File>() {
        public int compare(File arg0, File arg1) {
            if (arg0 == null) {
                if (arg1 == null) {
                    return 0;
                }
                //null is > than everything else
                return 1;
            }
            //compare by name
            int nameCmp = arg0.getName().compareTo(arg1.getName());
            if (nameCmp != 0) {
                return nameCmp;
            }
            //if the names are identical, sort by job number
            //note: results may vary because there is no guarantee that the job numbers are in order
            return arg0.getPath().compareTo( arg1.getPath() );
        }
    };

    final private Comparator<File> filepathComparator = new Comparator<File>() {
        public int compare(File arg0, File arg1) {
            if (arg0 == null) {
                if (arg1 == null) {
                    return 0;
                }
                //null is > than everything else
                return 1;
            }
            return arg0.getPath().compareTo( arg1.getPath() );
        }
    };
    
//    final private Comparator<File> timestampComparator = new Comparator<File>() {
//        public int compare(File arg0, File arg1) {
//            if (arg0 == null) {
//                if (arg1 == null) {
//                    return 0;
//                }
//                //null is > than everything else
//                return 1;
//            }
//            long t0 = arg0.lastModified();
//            long t1 = arg1.lastModified();
//            if (t0 < t1) {
//                return -1;
//            }
//            if (t0 > t1) {
//                return 1;
//            }
//            return 0;
//        }
//    };
    
    final private Comparator<GpFilePath> gpFilePathComparator = new Comparator<GpFilePath>() {
        public int compare(GpFilePath arg0, GpFilePath arg1) {
            return filenameComparator.compare(arg0.getServerFile(), arg1.getServerFile());
        }
    };

    /**
     * Gather all of the result files into a filelist, and output that list to a file.
     * 
     * Output a filelist to a file.
     * 
     * @return
     */
    public GpFilePath writeFilelist() throws Exception {
        FileFilter nullFileFilter = null;
        return writeFilelist(nullFileFilter);
    }

    public GpFilePath writeFilelist(FileFilter fileFilter) throws Exception {
        String filelistFilename = "" + rootJobInfo.getJobNumber() + ".filelist.txt";
        return writeFilelist(filelistFilename, fileFilter);
    }

    public GpFilePath writeFilelist(String outputFilename, FileFilter fileFilter) throws Exception {
        List<GpFilePath> resultFilePaths = new ArrayList<GpFilePath>();
        for(ParameterInfo outputParam : allResultFiles) {
            GpFilePath gpFilePath = getFilePath(outputParam);
            //apply filter
            if (fileFilter == null || fileFilter.accept(gpFilePath.getServerFile())) {
                resultFilePaths.add(gpFilePath);
            }
        }
        //sort alphabetically
        Collections.sort(resultFilePaths, gpFilePathComparator);
        
        //output the filelist, one fully qualified path per line
        GpFilePath outputFilePath = new JobResultFile(rootJobInfo, new File(outputFilename));

        File output = outputFilePath.getServerFile();
        writeFileList(output, resultFilePaths, false);
        return outputFilePath;
    }
    
    /**
     * Rule for extracting a GpFilePath from a ParameterInfo.
     * 
     * @param outputParam
     * @return
     */
    private GpFilePath getFilePath(ParameterInfo outputParam) {
        //circa gp-3.3.3 and earlier, value is of the form, <jobid>/<filepath>, e.g. "1531/Hind_0001.snp"
        GpFilePath gpFilePath = null;
        String pathInfo = "/" + outputParam.getValue();
        try {
            gpFilePath = new JobResultFile(pathInfo);
        }
        catch (Exception e) {
            log.error(e);
        }
        return gpFilePath;
    }
    
    public static void writeFileList(File output, List<GpFilePath> files, boolean writeTimestamp) throws IOException {
        FileWriter writer = null;
        BufferedWriter out = null;
        try {
            writer = new FileWriter(output);
            out = new BufferedWriter(writer);
            for(GpFilePath filePath : files) {
                File file = filePath.getServerFile();
                if (writeTimestamp) {
                    out.write("timestamp="+file.lastModified());
                    out.write(" date="+new Date(file.lastModified())+" ");
                }
                out.write(file.getAbsolutePath());
                out.newLine();
            }
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }
}


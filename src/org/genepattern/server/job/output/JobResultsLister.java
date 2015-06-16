/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

public class JobResultsLister extends SimpleFileVisitor<Path> {
    private static final Logger log = Logger.getLogger(JobResultsLister.class);
    
    public static PathMatcher initIgnoredPaths() {
        final FileSystem fs=FileSystems.getDefault();

        CompositePathMatcher c = new CompositePathMatcher();
        c.add( fs.getPathMatcher("glob:**/.svn**"));
        c.add( fs.getPathMatcher("glob:**/*~"));
        return c;
    }
    
    /**
     * Hard coded list of ignore paths which are never recorded into the database.
     * <pre>  .svn folders, files ending in '~'.
     * </pre>
     */
    public static final PathMatcher DEFAULT_IGNORED_PATHS=initIgnoredPaths();

    private final String jobId;
    private final File workingDir;
    private final Path workingDirPath;
    private final GpFileTypeFilter fileFilter;
    final List<JobOutputFile> allFiles=new ArrayList<JobOutputFile>();
    final List<JobOutputFile> hiddenFiles=new ArrayList<JobOutputFile>();
    private boolean walkHiddenDirectories=true;
    private PathMatcher ignoredPaths=DEFAULT_IGNORED_PATHS;

    public JobResultsLister(String jobId, File jobDir) {
        this(jobId, jobDir, null);
    }

    public JobResultsLister(String jobId, File workingDir, GpFileTypeFilter fileFilter) throws NumberFormatException {
        this.jobId=jobId;
        this.workingDir=workingDir;
        this.workingDirPath=workingDir.toPath();
        this.fileFilter=fileFilter;
    }
    
    public void walkFiles() throws IOException {
        Files.walkFileTree(workingDirPath, this);
    }

    public List<JobOutputFile> getHiddenFiles() {
        return Collections.unmodifiableList(hiddenFiles);
    }
    
    public List<JobOutputFile> getOutputFiles() {
        return Collections.unmodifiableList(allFiles);
    }
    
    public void sortByPath() {
        // sort by relative pathname
        Collections.sort(allFiles, new Comparator<JobOutputFile>() {
            @Override
            public int compare(JobOutputFile o1, JobOutputFile o2) {
                return o1.getPath().toLowerCase().compareTo(o2.getPath().toLowerCase());
            }
        });
    }

    public void sortByLastModified() {
        // sort by relative pathname
        Collections.sort(allFiles, new Comparator<JobOutputFile>() {

            @Override
            public int compare(JobOutputFile o1, JobOutputFile o2) {
                return o1.getLastModified().compareTo(o2.getLastModified());
            }
        });
    }

    private boolean checkAdd(Path file, BasicFileAttributes attrs) throws IOException {
        if (ignoredPaths.matches(file)) {
            return false;
        }
        
        File relativeFile=workingDirPath.relativize(file).toFile();
        GpFileType gpFileType=null;
        if (fileFilter!=null) {
            gpFileType=fileFilter.getGpFileType(workingDir, relativeFile, attrs);
        }
        JobOutputFile jobOutputFile = JobOutputFile.from(jobId, workingDir, relativeFile, attrs, gpFileType);
        allFiles.add(jobOutputFile);
        if (jobOutputFile.isHidden()) {
            hiddenFiles.add(jobOutputFile);
            return false;
        }
        return true;
    }
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        boolean isHiddenDir = ! checkAdd(dir, attrs);
        if (isHiddenDir && !walkHiddenDirectories) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        boolean added=checkAdd(file,attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (dir.equals(workingDir)) {
            //search complete
            sortByPath();
        }
        return FileVisitResult.CONTINUE;
    }

}

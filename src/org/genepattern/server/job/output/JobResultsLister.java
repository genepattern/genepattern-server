package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.util.JobResultsFilenameFilter;

public class JobResultsLister extends SimpleFileVisitor<Path> {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private final String jobId;
    private final Path workingDir;
    private final JobResultsFilenameFilter filenameFilter;
    final List<JobOutputFile> out=new ArrayList<JobOutputFile>();
    final List<JobOutputFile> hidden=new ArrayList<JobOutputFile>();
    private boolean walkHiddenDirectories=false;

    JobResultsLister(String jobId, File jobDir) {
        this.jobId=jobId;
        this.workingDir=jobDir.toPath();
        this.filenameFilter=null;
    }

    JobResultsLister(String jobId, File workingDirAsFile, JobResultsFilenameFilter filenameFilter) {
        this.jobId=jobId;
        this.workingDir=workingDirAsFile.toPath();
        this.filenameFilter=filenameFilter;
    }

    public void walkFiles() throws IOException {
        Files.walkFileTree(workingDir, this);
    }


    List<JobOutputFile> getOutputFiles() {
        return out;
    }
    
    List<JobOutputFile> getHiddenFiles() {
        return hidden;
    }

    public void sortByPath() {
        // sort by relative pathname
        Collections.sort(out, new Comparator<JobOutputFile>() {
            @Override
            public int compare(JobOutputFile o1, JobOutputFile o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        });
    }

    public void sortByLastModified() {
        // sort by relative pathname
        Collections.sort(out, new Comparator<JobOutputFile>() {

            @Override
            public int compare(JobOutputFile o1, JobOutputFile o2) {
                return o1.getLastModified().compareTo(o2.getLastModified());
            }
        });
    }

    private boolean checkAdd(Path file, BasicFileAttributes attrs) {
        Path rel=workingDir.relativize(file);
        Path parentPath=rel.getParent();
        File parentFile= parentPath == null ? null : parentPath.toFile();
        JobOutputFile jobOutputFile=JobOutputFile.from(jobId, rel, attrs);
        if (filenameFilter==null || filenameFilter.accept(parentFile, rel.getFileName().toString())) {
            out.add( jobOutputFile );
            return true;
        }
        jobOutputFile.setHidden(true);
        hidden.add( jobOutputFile );
        return false;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (dir.equals(workingDir)) {
            //don't include the working directory in the list of results
            return FileVisitResult.CONTINUE;
        }
        boolean isHiddenDir= ! checkAdd(dir, attrs);
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

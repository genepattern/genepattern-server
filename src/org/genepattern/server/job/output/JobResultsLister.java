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
import org.genepattern.server.util.JobResultsFilenameFilter;

public class JobResultsLister extends SimpleFileVisitor<Path> {
    private static final Logger log = Logger.getLogger(JobResultsLister.class);
    static class CompositePathMatcher implements PathMatcher {
        private List<PathMatcher> matchers=new ArrayList<PathMatcher>();
        
        public void add(PathMatcher pathMatcher) {
            matchers.add(pathMatcher);
        }

        @Override
        public boolean matches(Path path) {
            if (matchers==null) {
                return false;
            }
            for(final PathMatcher matcher : matchers) {
                if (matcher.matches(path)) {
                    return true;
                }
            }
            return false;
        }
    }
    
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
    public static PathMatcher ignoredPaths=initIgnoredPaths();


    private final String jobId;
    private final File workingDir;
    private final Path workingDirPath;
    private final JobResultsFilenameFilter filenameFilter;
    final List<JobOutputFile> out=new ArrayList<JobOutputFile>();
    final List<JobOutputFile> hidden=new ArrayList<JobOutputFile>();
    private boolean walkHiddenDirectories=false;

    public JobResultsLister(String jobId, File jobDir) {
        this(jobId, jobDir, null);
    }

    public JobResultsLister(String jobId, File workingDir, JobResultsFilenameFilter filenameFilter) {
        this.jobId=jobId;
        this.workingDir=workingDir;
        this.workingDirPath=workingDir.toPath();
        this.filenameFilter=filenameFilter;
    }
    
    public void walkFiles() throws IOException {
        Files.walkFileTree(workingDirPath, this);
    }


    public List<JobOutputFile> getHiddenFiles() {
        return Collections.unmodifiableList(hidden);
    }
    
    public List<JobOutputFile> getOutputFiles() {
        return Collections.unmodifiableList(out);
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

    private boolean checkAdd(Path file, BasicFileAttributes attrs) throws IOException {
        if (ignoredPaths.matches(file)) {
            return false;
        }
        
        File relativeFile=workingDirPath.relativize(file).toFile();
        JobOutputFile jobOutputFile=JobOutputFile.from(jobId, workingDir, relativeFile);
        
        if (filenameFilter==null || filenameFilter.accept(relativeFile.getParentFile(), relativeFile.getName())) {
            out.add( jobOutputFile );
            return true;
        }
        jobOutputFile.setHidden(true);
        hidden.add( jobOutputFile );
        return false;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (dir.equals(workingDirPath)) {
            //don't include the working directory in the list of results
            return FileVisitResult.CONTINUE;
        }
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

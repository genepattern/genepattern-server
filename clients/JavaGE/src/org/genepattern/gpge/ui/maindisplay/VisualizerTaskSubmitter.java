/*
 * VisualizerTaskSubmitter.java
 *
 * Created on April 1, 2003, 11:42 PM
 */

package org.genepattern.gpge.ui.maindisplay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.genepattern.analysis.OmnigeneException;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.client.AnalysisJob;
import org.genepattern.client.AnalysisService;
import org.genepattern.client.RequestHandler;
import org.genepattern.gpge.io.ServerFileDataSource;
import org.genepattern.io.StorageUtils;
import org.genepattern.util.GPConstants;
import org.genepattern.util.RunGenePatternTaskMain;
import org.genepattern.util.StringUtils;



/**
 * Processes only Visualizers
 * @author  kohm
 */
public class VisualizerTaskSubmitter implements org.genepattern.gpge.ui.tasks.TaskSubmitter{
    
    /** Creates a new instance of VisualizerTaskSubmitter 
     * Communicates the className via the TaskInfo as an Attribute
     */
    public VisualizerTaskSubmitter(final DataObjectBrowser browser) throws org.genepattern.analysis.PropertyNotFoundException{
        this.messenger = browser;
        try {
            final Properties p = org.genepattern.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
            final String url_string = p.getProperty("analysis.service.URL");
            if( url_string == null )
                throw new org.genepattern.analysis.PropertyNotFoundException("Cannot get the analysis service URL!");

            final java.net.URL server = new java.net.URL(url_string);
            this.host = server.getHost();
            System.out.println("VisualizerTaskSubmitter host='"+host+"'");
            this.port = String.valueOf(server.getPort());
            System.out.println("port='"+port+"'");
        } catch (java.io.IOException ioe) { // catches (java.net.MalformedURLException mfue)
            throw new IllegalStateException("Error: While creating URL of server string:\n"
            +ioe.getMessage());
        }
        
        this.classPath = ".";
    }

    /** determines if this submitter is acceptable for the AnalysisService  */
    public boolean check(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) {
        return isVisualizer(selectedService);
    }
    
    /** submits the task  which is to load a visualizer over the net
     * @returns null no job
     */
    public AnalysisJob submitTask(final AnalysisService selectedService, final int id, final ParameterInfo[] paramInfos, final RequestHandler handler) throws OmnigeneException, WebServiceException, java.io.IOException {
        //System.out.println("\n\ndebug from submitTask():");
        //new Exception().printStackTrace();
        final TaskInfo task_info = selectedService.getTaskInfo();
        final String taskName = task_info.getName();//"BluePinkOGram";
        System.out.println("\nTask name="+taskName);
        final String className = (String)task_info.getTaskInfoAttributes().get("className");
		  if("edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask".equals(className)) {
			  javax.swing.JOptionPane.showMessageDialog(org.genepattern.gpge.GenePattern.getDialogParent(), taskName + " is not a correctly defined Java visualizer.");
			  return null;
		  }
        messenger.setMessage("Running "+taskName);
        
        System.out.println("ParameterInfos:");
        
        final int limit = paramInfos.length;
        final String[] args = new String[limit];
        System.out.println("Num args="+limit+"\nParams:");
        for(int i = 0; i < limit; i++) {
            final ParameterInfo p_info = paramInfos[i];
            System.out.println(p_info);
            final String value = p_info.getValue();
            final Object mode = p_info.getAttributes().get(ParameterInfo.MODE);
            System.out.println("mode="+mode);
            // is it a server file 
            //if( p_info.isInputFile() && cached_mode.equals(mode) ) {
            if( ParameterInfo.CACHED_INPUT_MODE.equals(mode) ) {
                System.out.println("server file - value:\n"+value);
                final String f_name = ServerFileDataSource.getNameNoJunk(value);
                final File tmp_file = StorageUtils.createTempFileNoMung(f_name);
                final InputStream in = ServerFileDataSource.createRemoteFileInputStream(value);
                final FileOutputStream out = new FileOutputStream(tmp_file);
                StorageUtils.copyInputToOutputStream(in, out);
                args[i] = tmp_file.getAbsolutePath();
                in.close();
                out.close();
            } else if( ParameterInfo.URL_INPUT_MODE.equals(mode) ) { // a URL string
                System.out.println("url file - value:\n"+value);
                java.net.URL url = new java.net.URL(value);
                final String f_name = ServerFileDataSource.getJustFileName(value);
                final File tmp_file = StorageUtils.createTempFileNoMung(f_name);
                final FileOutputStream out = new FileOutputStream(tmp_file);
                final InputStream in = url.openStream();
                StorageUtils.copyInputToOutputStream(in, out);
                args[i] = tmp_file.getAbsolutePath();
                in.close();
                out.close();
            } else {
                System.out.println("Normal "+value);
                args[i] = value;
            }
        }
        System.out.println("end infos...");
        //final String[] args = new String[] {"/Users/kohm/data/res_files/test_KWO.res"};
        Throwable exception = null;
        
        try {
	    //final String fs = System.getProperty("file.separator");
            // run in this JVM
            //RunGenePatternTaskMain.run(className, args, taskName, host, port, classPath, false/*debug off*/);
            final String path_to_run = new File(PATH_TO_RUNGP).getAbsolutePath();
            // setup to run in separate JVM
            final String program = RunGenePatternTaskMain.class.getName();
//            final String[] cmd = new String[limit + 9];
//            cmd[0] = '"'+System.getProperty("java.home")+fs+"bin"+fs+"java\"";
//            cmd[1] = "-cp";
//            cmd[2] = '"'+path_to_run+'"';//PATH_TO_RUNGP;
//            cmd[3] = program;
//            cmd[4] = className;
//            cmd[5] = taskName;
//            cmd[6] = host;
//            cmd[7] = port;
//            cmd[8] = classPath;
//            System.arraycopy(args, 0, cmd, 10, limit);
            final List list = new ArrayList(limit + 9);
            //list.add('"'+System.getProperty("java.home")+fs+"bin"+fs+"java\"");
            //list.add(System.getProperty("java.home")+fs+"bin"+fs+"java");
            list.add(JAVA_PATH);
            list.add("-cp");
            //list.add('"'+path_to_run+'"');
            list.add(path_to_run);
            list.add(program);
            list.add(className);
            list.add(taskName);
            list.add(host);
            list.add(port);
            list.add(classPath);
            for(int i = 0; i < limit; i++) {
                list.add(args[i]);
            }
            final String[] cmd = (String[])list.toArray(new String[list.size()]);
            System.out.println("***** VisualizerTaskSubmitter Command Line:");
            //edu.mit.genome.debug.FieldDumper.printArray(cmd, System.out, " ");
            System.out.println("*****");
            final Runtime rt = Runtime.getRuntime();
            final Process proc = rt.exec(cmd);
            
            // any error message?
            final StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), taskName+"-ERR");            
            
            // any output?
            final StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), taskName+"-OUT");
                
            // kick them off
            new Thread(errorGobbler).start();
            new Thread(outputGobbler).start();
                                    
            // any error???
            new Thread(
                new org.genepattern.util.RunLater() {
                    protected final void runIt() throws Exception{
                        int exitVal = proc.waitFor();
                        if( messenger.getMessage().endsWith(taskName) )
                            messenger.setMessage(null);
                        
                       if( errorGobbler.tmp_file.length() > 0L ) {
                            final String error_text = StorageUtils.createStringFromContents(errorGobbler.tmp_file);
                            org.genepattern.gpge.GenePattern.showError(null, taskName +" error:\n"+ error_text);
                        }
                    }
                }
            ).start();
            
//        } catch (ClassNotFoundException cnfe) {
//            exception = cnfe;
//        } catch (java.lang.NoSuchMethodException nsme) {
//            exception = nsme;
//        } catch (IllegalAccessException iae) {
//            exception = iae;
        } catch (Throwable tw) {
            exception = tw;
        }
        
        if( exception != null ) {
            final String error = "Cannot Run GenePattern Task "+taskName
                                            +" as class '"+className+"'";
            org.genepattern.gpge.GenePattern.showError(null, error, exception);
            messenger.setMessage("Cannot Run GenePattern Task "+taskName);
            exception.printStackTrace();
            throw new WebServiceException(error, exception);
        }
        return null; 
    }
    
    // helper methods
    /** retrieves the class path 
     * note only works for Visualizer Tasks 
     */
    private String findClassPath(final AnalysisService selectedService, final ParameterInfo[] paramInfos) throws OmnigeneException{
        final int limit = paramInfos.length;
        System.out.println("ParameterInfo: ");
        for(int i = 0; i < limit; i++) {
            final ParameterInfo param_info = paramInfos[i];
            final String name = param_info.getName().trim();
            System.out.println("name ='"+name+"' label='"+param_info.getLabel()+"' description='"+param_info.getDescription()+"' value='"+param_info.getValue()+"'");
            if( CLASS_NAME_KEY.equalsIgnoreCase(name) ) 
                return param_info.getDescription();
        }
        throw new OmnigeneException("Cannot find the class path for the Visualizer, "
            + selectedService.getTaskInfo().getName()+"!");
    }
    /** determines if the task is a visualizer 
     */
    public static final boolean isVisualizer(final AnalysisService service) {
        return VISUALIZER.equalsIgnoreCase((String)service.getTaskInfo().getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
    }
    public static void main(String[] args) throws Throwable{
        System.out.println("test it");
//        final String home = System.getProperty("user.home");
//        final String separator = java.io.File.separator;
//        //edu.mit.genome.gp.ui.OmniView.setPropertyPath(home+separator+".gp"+separator+"resources"+separator);
//        System.setProperty("omnigene.conf", home+separator+".gp"+separator+"resources"+separator);
//        VisualizerTaskSubmitter submiter = new VisualizerTaskSubmitter();
//        submiter.submitTask(null, 0, null, null);
    }
    // fields
    /** the GPConstants.TASK_TYPE should return a value of "visualizer"
     * for Visualizers
     */
    protected static final String VISUALIZER = "visualizer";
    /** the class path key */
    protected static final String CLASS_NAME_KEY = "className";
    /** the classpath to the RunGenePatternTaskMain class */
    protected static final String PATH_TO_RUNGP;
    /** path to the javaw command */
    private static final String JAVA_PATH;
    /** where to report messages */
    private final DataObjectBrowser messenger;
    /** the current host */
    protected final String host;
    /** the host's port */
    protected final String port;
    /** the class path */
    protected final String classPath;
    
    /** static initializer Finds the path to the jar file or the dir where edu/ is*/
    static {
        // find command line path to java executable
        final String space_rep = "%20", space = " ";
        
        final String fs = System.getProperty("file.separator");
        String path = System.getProperty("java.home")+fs+"bin"+fs;
        if( new File(path, "javaw").exists() || new File(path, "javaw.exe").exists()) {
            path = path + "javaw";
            
        } else
            path = path + "java";
        // is there a space in the path and is it one of Microsoft's OS products then quote it
//        if( path.indexOf(' ') >= 0 && System.getProperty("os.name").indexOf("Windows") >= 0 )
//                path = '"'+path+'"';
        if( path.indexOf(space_rep) >= 0 )
            path = StringUtils.replaceAll(path, space_rep, space);
        JAVA_PATH = path;
        
        // find path to RunGenePatternTaskMain
        final java.net.URL rgptm = org.genepattern.gpge.util.JWhich.findClass(RunGenePatternTaskMain.class.getName());
        final String url_text = rgptm.toString();
        System.out.println("URL of path to RunGenePatternTaskMain="+url_text);
        String path_to_rungp = null;
        // is it a jar file
        int index = url_text.indexOf('!');
        
        final int f_i = url_text.indexOf("ile:");
        final int begin = (f_i < 0) ? 0 : f_i + 4;
        if( index >= 0 ) {
            path_to_rungp = url_text.substring(begin, index);
            System.out.println("Jar: "+path_to_rungp);
//            if( path_to_rungp.startsWith("file:") )
//                path_to_rungp = path_to_rungp.substring(7);
        } else {
            index = url_text.indexOf("edu");
            path_to_rungp = url_text.substring(begin, index)+'.';
            System.out.println("no jar: "+path_to_rungp);
        }
        if( path_to_rungp.indexOf(space_rep) >= 0 )
            path_to_rungp = StringUtils.replaceAll(path_to_rungp, space_rep, space);
        PATH_TO_RUNGP = path_to_rungp;
    }
    
    // I N N E R   C L A S S E S
    
    
    static class StreamGobbler extends org.genepattern.util.RunLater {
        private final InputStream is;
        //private final String type;
        public final  File   tmp_file;
        
        StreamGobbler(final InputStream is, final String type) throws IOException{
            this.is = is;
            //this.type = type;
            this.tmp_file = File.createTempFile(type, ".txt");
        }
        
        protected final void runIt() {
            try {
                final PrintWriter writer = new PrintWriter(new FileWriter(tmp_file));
                final InputStreamReader isr = new InputStreamReader(is);
                final BufferedReader br = new BufferedReader(isr);
                for(String line = br.readLine(); line != null; line = br.readLine()) {
                    writer.println(line);
                }
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }// end StreamGobbler
}// end VisualizerTaskSubmitter

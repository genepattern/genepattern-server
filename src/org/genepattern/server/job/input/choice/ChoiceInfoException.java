package org.genepattern.server.job.input.choice;


/**
 * Exceptions initializing the ChoiceInfo from either the manifest or a remote source.
 * @author pcarr
 *
 */
public class ChoiceInfoException extends Exception {

    /**
     * Optional representation of the status of the choice info,
     * to indicate to the end-user if there were any problems initializing the list 
     * of choices for the parameter.
     * 
     * @author pcarr
     *
     */
    public static class Status {
        public static enum Flag {
            OK,
            WARNING,
            ERROR
        }
        
        final private Flag flag;
        final private String message;
        
        public Status(final Flag flag) {
            this(flag,flag.toString());
        }
        public Status(final Flag flag, final String message) {
            this.flag=flag;
            this.message=message;
        }
        
        public Flag getFlag() {
            return flag;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public ChoiceInfoException(final Throwable t) {
        status=new Status(Status.Flag.ERROR, t.getLocalizedMessage());
    }
    
    public ChoiceInfoException(final String message) {
        status=new Status(Status.Flag.ERROR, message);
    }
    
    final Status status;
    public Status getStatus() {
        return status;
    }
    
    //    /**
//     * Types of errors:
//     * 1) ModuleManifest error, some problem in the manifest for example,
//     *    incorrect URL for ftpDir
//     *    incorrect format for 'choices' parameter
//     *    incorrect format for 'values' paramter
//     * 
//     * 2) ConnectionError, error connecting to remote server for listing file choices
//     * 
//     * 3) ConnectionTimeout, timeout waiting for remote server listing of file choices
//     *    Hint: try again soon.
//     * 
//     * 4) GpServerError, probably a bug in the GP server code
//     * 
//     * Java bean representation for initialization error, so that we can include a meaningful error
//     * message to the end user if there were problems initializing the dynamic choice list.
//     * 
//     * @author pcarr
//     *
//     */
//    public static enum InitError {
//        /**
//         * Some problem in the module manifest, for example if the 'ftpDir' in the manifest is not a valid URL.
//         * Contact the author of the module.
//         */
//        ModuleManifestError,
//        /**
//         * Some type of connection error when initializing the list of choices from the remote server.
//         */
//        ConnectionError,
//        /**
//         * It took too long to connection or otherwise get the list of choices from the remote server.
//         * Try back soon. Similar to a 504 Gateway Timeout.
//         */
//        ConnectionTimeout,
//        /**
//         * GpServerError, probably a bug in the GP server code.
//         */
//        GpServerError; 
//    }


}

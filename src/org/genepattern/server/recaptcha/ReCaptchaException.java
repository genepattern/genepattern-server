package org.genepattern.server.recaptcha;

public class ReCaptchaException extends java.lang.Exception {
    public ReCaptchaException(final String message) {
        super(message);
    }
    
    public ReCaptchaException(final Throwable t) {
        super(t);
    }
    
    public ReCaptchaException(final String message, final Throwable t) {
        super(message, t);
    }
}
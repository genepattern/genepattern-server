package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import junit.framework.TestCase;

public class UploadReceiverTest extends TestCase {
    
    UploadReceiver theTest = new UploadReceiver();
    Mockery context = new Mockery();
    
    public UploadReceiverTest() {
        
    }
    
    public static void main(String[] args) {
        JUnitCore.main("org.genepattern.server.webapp.UploadReceiverTest");
    }
    
    private List<FileItem> buildTestParameterList() {
        List<FileItem> list = new ArrayList<FileItem>();
        
        // Build item #1
        final FileItem item1 = context.mock(FileItem.class, "fileItem1");
        context.checking(new Expectations() {{
            allowing(item1).isFormField(); will(returnValue(false));
            allowing(item1).getFieldName(); will(returnValue("testField1"));
            allowing(item1).getString(); will(returnValue("testFalse1"));
        }});
        list.add(item1);
        
        // Build item #2
        final FileItem item2 = context.mock(FileItem.class, "fileItem2");
        context.checking(new Expectations() {{
            allowing(item2).isFormField(); will(returnValue(true));
            allowing(item2).getFieldName(); will(returnValue("testField2"));
            allowing(item2).getString(); will(returnValue("testTrue2"));
        }});
        list.add(item2);
        
        // Build item #3
        final FileItem item3 = context.mock(FileItem.class, "fileItem3");
        context.checking(new Expectations() {{
            allowing(item3).isFormField(); will(returnValue(false));
            allowing(item3).getFieldName(); will(returnValue("testField3"));
            allowing(item3).getString(); will(returnValue("testFalse3"));
        }});
        list.add(item3);
        
        return list;
    }
    
    private HttpServletRequest buildTestRequest() {
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        final HttpSession session = buildTestSession();
        
        // Expectations
        context.checking(new Expectations() {{
            allowing(request).getSession(); will(returnValue(session));
            allowing(request).getParameter("paramId"); will(returnValue("1234"));
        }});
        
        //request.getSession().setAttribute("paramId", "1234");
        //request.getSession().setAttribute("1234", "/writeDirectory");

        return request;
    }
    
    private HttpSession buildTestSession() {
        final HttpSession session = context.mock(HttpSession.class);
        
        // Expectations
        context.checking(new Expectations() {{
            allowing(session).getAttribute("1234"); will(returnValue("/writeDirectory"));
            allowing(session).getAttribute("userid"); will(returnValue("admin"));
        }});
        
        return session;
    }
    
    @Test
    public void testReceiveSmallFile() {
        // TODO: Implement
    }
    
    @Test
    public void testReceiveLargeFile() {
        // TODO: Implement
    }
    
    @Test
    public void testGetParameter() {
        List<FileItem> list = buildTestParameterList();
        
        String param = theTest.getParameter(list, "testField2");
        assertEquals(param, "testTrue2");
        
        param = theTest.getParameter(list, "testField3");
        assertEquals(param, null);
    }
    
    @Test
    public void testGetWriteDirectory() throws IOException {
        List<FileItem> params = buildTestParameterList();
        HttpServletRequest request = buildTestRequest();
        
        String dir = theTest.getWriteDirectory(request, params);
        assertEquals(dir, "/writeDirectory");
    }
}

package org.genepattern.server.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class UploadReceiverTest {
    
    final static String TMPDIR = System.getProperty("java.io.tmpdir");
    UploadReceiver theTest = new UploadReceiver();
    Mockery context = new Mockery();
    
    @AfterClass
    public static void removeFiles() {
        File file1 = new File(TMPDIR, "test.txt");
        File file2 = new File(TMPDIR, "test2.txt");
        file1.delete();
        file2.delete();
    }
    
    private List<FileItem> buildTestParameterList() throws Exception {
        List<FileItem> list = new ArrayList<FileItem>();
        
        // Build item #1
        final FileItem item1 = context.mock(FileItem.class, "fileItem1");
        context.checking(new Expectations() {{
            allowing(item1).isFormField(); will(returnValue(false));
            allowing(item1).getFieldName(); will(returnValue("testField1"));
            allowing(item1).getString(); will(returnValue("testFalse1"));
            allowing(item1).getName(); will(returnValue("test.txt"));
            allowing(item1).write(new File(TMPDIR, "test.txt"));
            allowing(item1).get(); will(returnValue(new byte[1]));
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
            allowing(item3).getName(); will(returnValue("test2.txt"));
            allowing(item3).write(new File(TMPDIR, "test2.txt"));
            allowing(item3).get(); will(returnValue(new byte[1]));
        }});
        list.add(item3);
        
        return list;
    }
    
//    private HttpServletRequest buildTestRequest() {
//        final HttpServletRequest request = context.mock(HttpServletRequest.class);
//        final HttpSession session = buildTestSession();
//        
//        // Expectations
//        context.checking(new Expectations() {{
//            allowing(request).getSession(); will(returnValue(session));
//            allowing(request).getParameter("paramId"); will(returnValue("1234"));
//        }});
//
//        return request;
//    }
//    
//    private HttpSession buildTestSession() {
//        final HttpSession session = context.mock(HttpSession.class);
//        
//        // Expectations
//        context.checking(new Expectations() {{
//            allowing(session).getAttribute("1234"); will(returnValue(TMPDIR));
//            allowing(session).getAttribute("userid"); will(returnValue("admin"));
//        }});
//        
//        return session;
//    }
    
//    @Test
//    public void testReceiveSmallFile() throws Exception {
//        HttpServletRequest request = buildTestRequest();
//        List<FileItem> postParameters = buildTestParameterList();
//        PrintWriter responseWriter = new PrintWriter(System.out);
//        
//        Context userContext = GpContext.getContextForUser("admin");
//        theTest.writeFile(userContext, request, postParameters, 0, 1, "admin"); 
//        
//        File file1 = new File(TMPDIR, "test.txt");
//        File file2 = new File(TMPDIR, "test2.txt");
//        Assert.assertTrue(file1.exists());
//        Assert.assertTrue(file2.exists());
//    }
    
    @Test
    public void testGetParameter() throws Exception {
        List<FileItem> list = buildTestParameterList();
        
        String param = theTest.getParameter(list, "testField2");
        Assert.assertEquals(param, "testTrue2");
        
        param = theTest.getParameter(list, "testField3");
        Assert.assertEquals(param, null);
    }
    
//    @Test
//    public void testGetWriteDirectory() throws Exception {
//        List<FileItem> params = buildTestParameterList();
//        HttpServletRequest request = buildTestRequest();
//        
//        Context userContext = GpContext.getContextForUser("admin");
//        File dir = theTest.getUploadDirectory(userContext, request);
//        Assert.assertEquals(dir.getCanonicalPath(), TMPDIR);
//    }
}

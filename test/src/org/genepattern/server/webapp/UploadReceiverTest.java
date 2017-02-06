/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.fileupload.FileItem;

import org.junit.Test;

public class UploadReceiverTest {
    UploadReceiver uploadReceiver = new UploadReceiver();
    
    private List<FileItem> buildTestParameterList() throws Exception {
        List<FileItem> list = new ArrayList<FileItem>();

        // Build item #1
        final FileItem item1 = mock(FileItem.class); //, "fileItem1");
        when(item1.isFormField()).thenReturn(false);
        when(item1.getFieldName()).thenReturn("testField1");
        when(item1.getString()).thenReturn("testFalse1");
        when(item1.getName()).thenReturn("test.txt");
        when(item1.get()).thenReturn(new byte[1]);
        list.add(item1);
        
        // Build item #2
        final FileItem item2 = mock(FileItem.class); //, "fileItem2");
            when(item2.isFormField()).thenReturn(true);
            when(item2.getFieldName()).thenReturn("testField2");
            when(item2.getString()).thenReturn("testTrue2");
        list.add(item2);
        
        // Build item #3
        final FileItem item3 = mock(FileItem.class); //, "fileItem3");
        when(item3.isFormField()).thenReturn(false);
        when(item3.getFieldName()).thenReturn("testField3");
        when(item3.getString()).thenReturn("testFalse3");
        when(item3.getName()).thenReturn("test2.txt");
        //when(item3.write(new File(TMPDIR, "test2.txt"));
        when(item3.get()).thenReturn(new byte[1]);
        list.add(item3);

        return list;
    }
    
    @Test
    public void getParameter_nullParameters() {
        List<FileItem> parameters=null;
        assertEquals(null, uploadReceiver.getParameter(parameters, "testField2"));
    }

    @Test
    public void getParameter_emptyParameters() {
        List<FileItem> parameters=Collections.emptyList();
        assertEquals(null, uploadReceiver.getParameter(parameters, "testField2"));
    }
    
    @Test
    public void testGetParameter() throws Exception {
        List<FileItem> parameters = buildTestParameterList();
        
        String param = uploadReceiver.getParameter(parameters, "testField2");
        assertEquals("testTrue2", param);
        
        param = uploadReceiver.getParameter(parameters, "testField3");
        assertEquals(param, null);
    }
    
}

package com.telenav.osv.upload;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author horatiuf
 */
public class UploadOperationFactoryTest {

    UploadOperationFactory uploadOperationFactory;

    @Before
    public void setUp() throws Exception {
        uploadOperationFactory = new UploadOperationFactoryImpl();
    }

    @After
    public void tearDown() throws Exception {
        uploadOperationFactory = null;
    }

    @Test
    public void createUploadSequenceOperation() throws Exception {
        //Mock

        //Test

        //Assert
    }

    @Test
    public void createUploadImageOperation() throws Exception {
        //Test

        //Assert
    }

    @Test
    public void createUploadVideoOperation() throws Exception {
        //Test

        //Assert
    }

    @Test
    public void createUploadFinishOperation() throws Exception {
        //Test

        //Assert
    }

    @Test
    public void createUploadMetadataOperation() throws Exception {
        //Test

        //Assert
    }

}
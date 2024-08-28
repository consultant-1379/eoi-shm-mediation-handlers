/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.shm.eoi.error;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.mediation.shm.ecim.common.MoActionResponseSender;
import com.ericsson.oss.mediation.shm.eoi.common.ReadResponseSender;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;

@RunWith(MockitoJUnitRunner.class)
public class ShmEoiErrorHandlerTest {

    @InjectMocks
    private ShmEoiErrorHandler objectUnderTest;
    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent componentEventMock;
    @Mock
    private Configuration configMock;
    @Mock
    private ReadResponseSender readResponseSenderMock;
    @Mock
    private MoActionResponseSender moActionResponseSender;

    private Map<String, Object> headers;

    private Map<String, Object> eventAttribute;

    private Map<String, Object> additionalInfo;

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.error.ShmEoiErrorHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit() {

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(headers);
        objectUnderTest.init(contextMock);
        verify(configMock, times(1)).getAllProperties();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.error.ShmEoiErrorHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit_ForNullConfiguration() {
        Mockito.doThrow(new NullPointerException()).when(contextMock).getEventHandlerConfiguration();
        objectUnderTest.init(contextMock);
        verify(contextMock, times(1)).getEventHandlerConfiguration();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.error.ShmEoiErrorHandler#destroy()}.
     */
    @Test
    public void testDestroy() {
        objectUnderTest.destroy();
    }

    @Test
    public void testGetHandlerCanonicalName() {
        Assert.assertEquals(ShmEoiErrorHandler.class.getCanonicalName(), objectUnderTest.getHandlerCanonicalName());
    }

    @Test
    public void testGetHandlerName() {
        Assert.assertEquals(ShmEoiErrorHandler.class.getName(), objectUnderTest.getHandlerName());
    }

    @Test
    public void testHandleError() {
        objectUnderTest.handleError("TEST_STRING", componentEventMock, System.currentTimeMillis());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.error.ShmEoiErrorHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent_SuccessForAction() {
        final String errorMessage = "Some error occurred.";
        eventAttribute = new HashMap<String, Object>();
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.OPERATION_TYPE, ShmMediationConstants.ACTION);
        eventAttribute.put(ShmMediationConstants.ORIGINAL_FLOW_ERROR, errorMessage);
        eventAttribute.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(1)).getHeaders();
        verify(moActionResponseSender, times(1)).sendErrorResponse(Mockito.anyMap(), Mockito.eq(errorMessage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent_SuccessForRead() {
        final String errorMessage = "Some error occurred.";
        eventAttribute = new HashMap<String, Object>();
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.OPERATION_TYPE, ShmMediationConstants.READ);
        eventAttribute.put(ShmMediationConstants.ORIGINAL_FLOW_ERROR, errorMessage);
        eventAttribute.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(1)).getHeaders();
        verify(readResponseSenderMock, times(1)).sendErrorResponse(Mockito.anyMap(), Mockito.eq(errorMessage));
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.error.ShmEoiErrorHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     */
    @Test
    public void testOnEvent_ForNullHeader() {
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(1)).getHeaders();
    }

}

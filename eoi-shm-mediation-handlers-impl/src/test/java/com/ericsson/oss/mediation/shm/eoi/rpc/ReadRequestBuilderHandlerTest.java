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
package com.ericsson.oss.mediation.shm.eoi.rpc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.cba.handlers.read.FilterBuilder;
import com.ericsson.oss.mediation.shm.eoi.common.ReadResponseSender;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;

/**
 * Junit test class for {@link ReadRequestBuilderHandler}.
 *
 * @author xnalman
 *
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ReadRequestBuilderHandlerTest {

    @InjectMocks
    private ReadRequestBuilderHandler objectUnderTest;

    @Mock
    private FilterBuilder filterBuilderMock;
    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent componentEventMock;
    @Mock
    private Configuration configMock;
    @Mock
    private ReadResponseSender readResponseSenderMock;
    @Mock
    private ReadFilterBuilder shmFilterBuilderMock;

    private Map<String, Object> headers;

    private Map<String, Object> eventAttribute;

    private Map<String, Object> additionalInfo;

    private String poNameSpace;

    private String poType;

    private String poVersion;

    private String fdn;

    private void mockHeader() {
        poNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        poType = "backup-manager";
        poVersion = "3.0.1";
        fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system,progress-report=0";
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.TYPE, poType);
        headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.NAMESPACE, poNameSpace);
        headers.put(ShmMediationConstants.VERSION, poVersion);
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
    }

    private void mockEventAttributes() {
        final List<String> attributesList = new ArrayList<String>();
        attributesList.add("action-id");
        attributesList.add("state");
        attributesList.add("result-info");
        attributesList.add("result");
        attributesList.add("progress-percentage");
        attributesList.add("progress-info");
        attributesList.add("action-name");
        attributesList.add("time-of-last-status-update");
        attributesList.add("time-action-completed");
        attributesList.add("time-action-started");
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.TYPE, poType);
        additionalInfo.put(ShmMediationConstants.JOB_TYPE, "BACKUP");
        additionalInfo.put(ShmMediationConstants.PLATFORM, "EOI");
        additionalInfo.put(ShmMediationConstants.ACTIVITY_NAME, "createbackup");
        additionalInfo.put(ShmMediationConstants.ACTIVITY_JOB_ID, 123L);
        eventAttribute = new HashMap<>();
        eventAttribute.put(ShmMediationConstants.MO_ATTRIBUTES, attributesList);
        eventAttribute.put(ShmMediationConstants.NAMESPACE, poNameSpace);
        eventAttribute.put(ShmMediationConstants.VERSION, poVersion);
        eventAttribute.put(ShmMediationConstants.FDN, fdn);
        eventAttribute.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
    }

    private String getTestFilter() {
        final String filter = "<brm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><brm-key>1</brm-key><backup-manager xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><id>log-system</id><progress-report xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><action-id>0</action-id><progress-report/></progress-report></backup-manager></brm>";
        return filter;
    }

    @Test
    public void testGetHandlerCanonicalName() {
        Assert.assertEquals(ReadRequestBuilderHandler.class.getCanonicalName(), objectUnderTest.getHandlerCanonicalName());
    }

    @Test
    public void testGetHandlerName() {
        Assert.assertEquals(ReadRequestBuilderHandler.class.getName(), objectUnderTest.getHandlerName());
    }

    @Test
    public void testHandleError() {
        objectUnderTest.handleError("TEST_STRING", componentEventMock, System.currentTimeMillis());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadRequestBuilderHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit() {
        mockHeader();
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(headers);

        objectUnderTest.init(contextMock);

        verify(configMock, times(1)).getAllProperties();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadRequestBuilderHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit_ForNullConfiguration() {
        Mockito.doThrow(new NullPointerException()).when(contextMock).getEventHandlerConfiguration();
        objectUnderTest.init(contextMock);
        verify(contextMock, times(1)).getEventHandlerConfiguration();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadRequestBuilderHandler#destroy()}.
     */
    @Test
    public void testDestroy() {
        objectUnderTest.destroy();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadRequestBuilderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     */
    @Test
    public void testOnEvent_Success() {
        mockHeader();
        mockEventAttributes();
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(filterBuilderMock.prepareFilter(fdn, (List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES))).thenReturn(getTestFilter());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(2)).getHeaders();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadRequestBuilderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     */
    @Test(expected = EventHandlerException.class)
    public void testOnEvent_ForNullHeader() {
        mockHeader();
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        Mockito.doThrow(new NullPointerException()).when(componentEventMock).getHeaders();

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(1)).getHeaders();
        verify(readResponseSenderMock, times(1)).sendErrorResponse(headers, new NullPointerException().getMessage());
    }

}

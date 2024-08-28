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
package com.ericsson.oss.mediation.shm.eoi.reader;

import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerConstants.HandlerAttributeKey.NETCONF_SESSION_OPERATIONS_STATUS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.com.provider.operation.ComNetconfOperationResult;
import com.ericsson.oss.mediation.cba.handlers.read.ModelServiceHelper;
import com.ericsson.oss.mediation.cba.handlers.read.ReadNonPersistenceAttributeParser;
import com.ericsson.oss.mediation.cba.handlers.utility.InstrumentationHelper;
import com.ericsson.oss.mediation.cba.handlers.utility.SyncConstant;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.shm.eoi.common.ReadResponseSender;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconManagerConstants;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

/**
 * Junit test class for {@link AttributesReaderHandler}.
 *
 * @author xnalman
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AttributesReaderHandlerTest {

    @InjectMocks
    private AttributesReaderHandler objectUnderTest;

    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private Configuration configMock;
    @Mock
    private ComponentEvent componentEventMock;
    @Mock
    private NetconfManager netconfManagerMock;
    @Mock
    private ModelServiceHelper modelServiceHelperMock;
    @Mock
    private NetconfResponse netconfResponseMock;
    @Mock
    private ComNetconfOperationResult comNetconfOperationResultMock;
    @Mock
    private InstrumentationHelper instrumentationHelperMock;
    @Mock
    private ReadNonPersistenceAttributeParser parserMock;
    @Mock
    private ReadResponseSender readResponseSenderMock;

    //@Mock private CbaReadHandlerInstrumentation instrumentationBeanMock;

    private Map<String, Object> headers;
    private Map<String, Object> additionalInfo;
    private Map<String, Object> eventAttribute;
    private String moNameSpace;
    private String moType;
    private String moVersion;
    private String fdn;

    private static final String EVENT_TYPE = "readEventType";
    private static final String EVENT_BASED = "EVENT_BASED";

    private void mockHeader() {
        moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        moType = "backup-manager";
        moVersion = "3.0.1";
        fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system,progress-report=0";
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.TYPE, moType);
        headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.ACTIVITY_JOB_ID, 123L);
    }

    private void mockEventAttributes() {
        moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        moType = "backup-manager";
        moVersion = "3.0.1";
        fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system,progress-report=0";
        final List<String> attributesList = new ArrayList<String>();
        final String filter = getTestFilter();
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
        additionalInfo.put(ShmMediationConstants.TYPE, moType);
        additionalInfo.put(ShmMediationConstants.JOB_TYPE, "BACKUP");
        additionalInfo.put(ShmMediationConstants.PLATFORM, "EOI");
        additionalInfo.put(ShmMediationConstants.ACTIVITY_NAME, "createbackup");
        eventAttribute = new HashMap<>();
        eventAttribute.put(NetconManagerConstants.NETCONF_MANAGER_ATTR, netconfManagerMock);
        eventAttribute.put(ShmMediationConstants.MO_ATTRIBUTES, attributesList);
        eventAttribute.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        eventAttribute.put(ShmMediationConstants.VERSION, moVersion);
        eventAttribute.put(ShmMediationConstants.FDN, fdn);
        eventAttribute.put(ShmMediationConstants.FILTER, filter);
        eventAttribute.put(EVENT_TYPE, EVENT_BASED);
        eventAttribute.put(ShmMediationConstants.ACTIVITY_JOB_ID, 123L);
        eventAttribute.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.NONE);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
        eventAttribute.put(NETCONF_SESSION_OPERATIONS_STATUS, netconfSessionOperationsStatus);
    }

    private String getTestFilter() {
        final String filter = "<brm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><brm-key>1</brm-key><backup-manager xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><id>log-system</id><progress-report xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\"><action-id>0</action-id><progress-report/></progress-report></backup-manager></brm>";
        return filter;
    }

    @Test
    public void testGetHandlerCanonicalName() {
        Assert.assertEquals(AttributesReaderHandler.class.getCanonicalName(), objectUnderTest.getHandlerCanonicalName());
    }

    @Test
    public void testGetHandlerName() {
        Assert.assertEquals(AttributesReaderHandler.class.getName(), objectUnderTest.getHandlerName());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit_ForSuccess() {
        mockHeader();
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(headers);
        objectUnderTest.init(contextMock);
        verify(contextMock, Mockito.times(1)).getEventHandlerConfiguration();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit_ForNullConfiguration() {
        when(contextMock.getEventHandlerConfiguration()).thenReturn(null);
        objectUnderTest.init(contextMock);
        verify(contextMock, times(1)).getEventHandlerConfiguration();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#destroy()}.
     */
    @Test
    public void testDestroy() {
        objectUnderTest.destroy();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent_ForSuccess() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        //   verify(modelServiceHelperMock, times(1)).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        //        verify(readResponseSenderMock, times(1)).sendResponse(anyMap(), eq(attributeMap));
        //        verify(modelServiceHelperMock, times(1)).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        //        verify(netconfResponseMock, times(1)).isError();
        //        verify(netconfResponseMock, times(3)).getData();
        //        verify(netconfResponseMock, never()).getErrorMessage();
        //        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        //        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        //        verify(instrumentationHelperMock, times(1)).addReqCrudProcessTime(Matchers.anyLong());
        //        verify(instrumentationHelperMock, times(1)).addResCrudProcessTime(Matchers.anyLong());
        //        verify(parserMock, times(1)).parseData(anyString());
        //        verify(readResponseSenderMock, times(1)).sendResponse(anyMap(), eq(attributeMap));
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithNullInputEventHeader() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(null);
        //doThrow(new NullPointerException()).when(componentEventMock).getHeaders().get(anyString());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(3)).getHeaders();
        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(modelServiceHelperMock, never()).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, never()).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithEmptyInputEventHeader() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(new HashMap<String, Object>());
        //doThrow(new NullPointerException()).when(componentEventMock).getHeaders().get(anyString());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(componentEventMock, times(5)).getHeaders();
        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(modelServiceHelperMock, never()).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, never()).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfManagerAsNullFromHeader() throws NetconfManagerException {
        mockEventAttributes();
        eventAttribute.put(NetconManagerConstants.NETCONF_MANAGER_ATTR, null);
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForPrimaryKeyAttributesAsNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(modelServiceHelperMock.getPrimaryKeyAttribute(anyString(), anyString(), anyString())).thenReturn(null);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForPrimaryKeyAttributesAsEmpty() throws NetconfManagerException {
        mockEventAttributes();
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] {}));

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent_ForExceptionDuringSendResponse() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);
        doThrow(new IllegalStateException()).when(readResponseSenderMock).sendResponse(headers, attributeMap);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        //        verify(parserMock, times(1)).parseData(anyString());
        // verify(readResponseSenderMock, times(1)).sendResponse(anyMap(), eq(attributeMap));
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNullFilter() throws NetconfManagerException {
        mockEventAttributes();
        eventAttribute.put(ShmMediationConstants.FILTER, null);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForEmptyFilter() throws NetconfManagerException {
        mockEventAttributes();
        eventAttribute.put(ShmMediationConstants.FILTER, "");

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfResponseIsNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(null);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfResponseIsError() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(true);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfResponseIsErrorTimeout() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(true);
        when(netconfResponseMock.getErrorCode()).thenReturn(SyncConstant.TIMEOUT_ERROR);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfResponseDataIsNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn(null);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForNetconfResponseDataIsEmpty() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("");

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFilterParserAttributeMapIsNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");

        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(null);
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        // verify(parserMock, times(1)).parseData(anyString());
        //  verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        //  verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFilterParserAttributeMapIsEmpty() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");

        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(new HashMap<String, Object>());
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        //     verify(parserMock, times(1)).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForPrimaryKeyAttributesAsNullAndResponseIsAlsoNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(null);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(anyString(), anyString(), anyString())).thenReturn(null);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);
        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForComponentEventHeaderAsNonEmpty() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.MEDIATION_TASK_REQUEST, "DUMMY_VALUE");
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithNetconfErrorMessageAsError() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(true);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(netconfResponseMock.getErrorMessage()).thenReturn("Test Error Occurred.");
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());

    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithNetconfErrorMessageAsNull() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(true);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(netconfResponseMock.getErrorMessage()).thenReturn(null);
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithNetconfErrorMessageAsEmpty() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(true);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(netconfResponseMock.getErrorMessage()).thenReturn("");
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithNullFdn() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        eventAttribute.put(ShmMediationConstants.FDN, null);
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test
    public void testOnEvent_ForFailureWithIncorrectFdn() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        fdn = "BrMBrmBackupManagerBrmBackup";
        eventAttribute.put(ShmMediationConstants.FDN, fdn);
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);
        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        //verify(readResponseSenderMock, times(1)).sendErrorResponse(eq(headers), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test()
    public void testOnEvent_ForFailureWithNetconfFailureBeforeCurrentHandler() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.OPERATION_FAILED);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
        eventAttribute.put(NETCONF_SESSION_OPERATIONS_STATUS, netconfSessionOperationsStatus);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        //doNothing().when(instrumentationBeanMock).increaseDpsCounterForSuccessfulRead();
        //doNothing().when(instrumentationBeanMock).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());

        objectUnderTest.init(contextMock);
        final ComponentEvent actualComponentEvent = objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(modelServiceHelperMock, never()).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(parserMock, never()).parseData(anyString());
        //verify(instrumentationBeanMock, never()).increaseDpsCounterForSuccessfulRead();
        //verify(instrumentationBeanMock, never()).increaseNumberOfAttributesRead(((List<String>) eventAttribute.get(ShmMediationConstants.MO_ATTRIBUTES)).size());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test()
    public void testOnEvent_ForFailureWithNetconfFailureBeforeCurrentHandlerAndReasonAsSkipAll() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();

        final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.SKIP_ALL);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
        eventAttribute.put(NETCONF_SESSION_OPERATIONS_STATUS, netconfSessionOperationsStatus);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, never()).get(any(Filter.class));
        verify(modelServiceHelperMock, never()).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(parserMock, never()).parseData(anyString());
        verify(instrumentationHelperMock, never()).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test()
    public void testOnEvent_ForSuccessWithNetconfManagerAlreadyConnected() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        when(netconfManagerMock.getStatus()).thenReturn(NetconfConnectionStatus.CONNECTED);
        when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        //    verify(modelServiceHelperMock, times(1)).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, times(1)).isError();
        //      verify(netconfResponseMock, times(3)).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, times(1)).incrementResponseCount();
        //    verify(instrumentationHelperMock, times(1)).addReqCrudProcessTime(Matchers.anyLong());
        //    verify(instrumentationHelperMock, times(1)).addResCrudProcessTime(Matchers.anyLong());
        //   verify(parserMock, times(1)).parseData(anyString());
        //  verify(readResponseSenderMock, times(1)).sendResponse(anyMap(), eq(attributeMap));
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.eoi.reader.AttributesReaderHandler#onEvent(com.ericsson.oss.itpf.common.event.ComponentEvent)}.
     *
     * @throws NetconfManagerException
     */
    @Test()
    public void testOnEvent_ForFailureWithExceptionInNetconfManagerGet() throws NetconfManagerException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("majorType", 1);
        attributeMap.put("minorType", 2);

        when(contextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttribute);
        when(componentEventMock.getHeaders()).thenReturn(eventAttribute);

        //when(netconfManagerMock.get(any(Filter.class))).thenReturn(netconfResponseMock);
        doThrow(new NullPointerException()).when(netconfManagerMock).get(any(Filter.class));

        when(modelServiceHelperMock.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));
        when(netconfResponseMock.isError()).thenReturn(false);
        when(netconfResponseMock.getData()).thenReturn("test");
        doNothing().when(instrumentationHelperMock).incrementRequestCount();
        doNothing().when(instrumentationHelperMock).incrementResponseCount();
        doNothing().when(instrumentationHelperMock).addReqCrudProcessTime(Matchers.anyLong());
        when(parserMock.getAttributeMap()).thenReturn(attributeMap);

        objectUnderTest.init(contextMock);
        objectUnderTest.onEvent(componentEventMock);

        verify(netconfManagerMock, times(1)).get(any(Filter.class));
        verify(modelServiceHelperMock, never()).getPrimaryKeyAttribute(eq(moType), eq(moNameSpace), eq(moVersion));
        verify(netconfResponseMock, never()).isError();
        verify(netconfResponseMock, never()).getData();
        verify(netconfResponseMock, never()).getErrorMessage();
        verify(instrumentationHelperMock, times(1)).incrementRequestCount();
        verify(instrumentationHelperMock, never()).incrementResponseCount();
        verify(instrumentationHelperMock, never()).addReqCrudProcessTime(Matchers.anyLong());
        verify(instrumentationHelperMock, never()).addResCrudProcessTime(Matchers.anyLong());
        verify(parserMock, never()).parseData(anyString());
        verify(readResponseSenderMock, times(1)).sendErrorResponse(anyMap(), anyString());
    }

}

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
package com.ericsson.oss.mediation.shm.eoi.common;

import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerConstants.HandlerAttributeKey.NETCONF_SESSION_OPERATIONS_STATUS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.ericsson.oss.mediation.cba.handlers.utility.InstrumentationHelper;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.mediation.adapter.transformer.ModelTransformer;
import com.ericsson.oss.mediation.adapter.transformer.converter.DataTypeConversionException;
import com.ericsson.oss.mediation.cba.handlers.read.ModelServiceHelper;
import com.ericsson.oss.mediation.cba.handlers.read.ReadNonPersistenceAttributeParser;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.shm.eoi.common.SubTreeFilter;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.mediation.shm.eoi.rpc.ReadFilterBuilder;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconManagerConstants;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

@RunWith(MockitoJUnitRunner.class)
public class EoiReadRequestServiceTest {

    @InjectMocks
    private EoiReadRequestService objectUnderTest;

    @Mock
    private ComponentEvent componentEventMock;

    @Mock
    private ReadFilterBuilder readFilterBuilder;

    @Mock
    private NetconfManager netconfManagerMock;

    @Mock
    private ModelServiceHelper modelServiceHelperMock;

    @Mock
    private NetconfResponse netconfResponseMock;

    @Mock
    private ModelServiceHelper modelServiceHelper;

    @Mock
    private ModelTransformer modelTransformer;

    @Mock
    InstrumentationHelper InstrumentationHelper;

    @Mock
    private ReadNonPersistenceAttributeParser attributeParser;

    @Mock
    private ManagedObject managedElementMock;

    @Mock
    Filter filter;

    private Map<String, Object> headers;
    private Map<String, Object> additionalInfo;
    private Map<String, Object> responseAttributes;
    private Map<String, Object> eventAttribute;
    private String moNameSpace;
    private String moType;
    private String moVersion;
    private String fdn;

    private static final String EVENT_TYPE = "readEventType";
    private static final String EVENT_BASED = "EVENT_BASED";

    public static final String ACTION_FILTER_RESPONSE ="<brm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
    + "<brm-key>1</brm-key>"
    + "<backup-manager xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
    + "<id>log-system</id>"
    + "<progress-report xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
    + "<action-id>0</action-id>"
    + "<progress-report/>"
    + "</progress-report>"
    + "</backup-manager>"
    + "</brm>";

    private void mockHeader() {
        moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        moType = "backup-manager";
        moVersion = "3.0.1";
        fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system,progress-report=0";

        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.TYPE, moType);
        additionalInfo.put(ShmMediationConstants.ACTIVITY_NAME, "createbackup");
        additionalInfo.put(ShmMediationConstants.JOB_TYPE, "NODE_HEALTH_CHECK");
        List<String> moAttributes = new ArrayList<>();
        moAttributes.add("progress-report");
        additionalInfo.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);

        headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        headers.put(ShmMediationConstants.JOB_TYPE, "NODE_HEALTH_CHECK");
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);
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
        additionalInfo.put(ShmMediationConstants.JOB_TYPE, "NODE_HEALTH_CHECK");
        additionalInfo.put(ShmMediationConstants.PLATFORM, "EOI");
        additionalInfo.put(ShmMediationConstants.ACTIVITY_NAME, "createbackup");
        additionalInfo.put(ShmMediationConstants.MO_ATTRIBUTES, "progress-report");
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

    private String getMoTypeFromMoFdn(final String moFdn) throws EventHandlerException {
        return moFdn.substring(moFdn.lastIndexOf(ShmMediationConstants.DELIMITER_COMMA) + 1, moFdn.lastIndexOf(ShmMediationConstants.DELIMITER_EQUAL));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetReadFilter() {
        mockHeader();
        mockEventAttributes();
        final String expectedFilter = getTestFilter();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);

        final List<String> attributes = objectUnderTest.getRequestAttributesFromActionRequest(additionalAttributes);
        final String filter = objectUnderTest.getReadFilter(fdn, componentEventMock);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, filter);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetReadFilterForCatch() {
        mockHeader();
        mockEventAttributes();
        final String expectedFilter = getTestFilter();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        when(componentEventMock.getHeaders()).thenReturn(null);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);

        final List<String> attributes = objectUnderTest.getRequestAttributesFromActionRequest(additionalAttributes);
        final String filter = objectUnderTest.getReadFilter(fdn, componentEventMock);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, filter);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsActionRunning() {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        final String expectedFilter = getTestFilter();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);

        final List<String> attributes = objectUnderTest.getRequestAttributesFromActionRequest(additionalAttributes);
        final String filter = objectUnderTest.getReadFilter(fdn, componentEventMock);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, filter);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);

    }

    @Test(expected=EventHandlerException.class)
    public void testPrepareResponseForNull() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        //final String activityName = additionalAttributes.get(ShmMediationConstants.ACTIVITY_NAME);

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
    }

    @Test
    public void testPrepareResponseForNotNull() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "create-backup");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        List<String> moAttributes = new ArrayList<>();
        moAttributes.add("testprogress-report");
        additionalAttributes.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);
        //additionalAttributes.put(expectedFilter, additionalAttributes)
        //final String activityName = additionalAttributes.get(ShmMediationConstants.ACTIVITY_NAME);

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(attributeParser.getAttributeMap()).thenReturn(additionalAttributes);
        //when(dpsHelperMock.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);

        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(modelTransformer.transformToDpsByData(Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(Map.class),Mockito.any(Boolean.class),Mockito.any(String.class))).thenReturn(newTransformedAttributes);

        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
        final String actioName = "create-backup";
        response.put(ShmMediationConstants.ASYNC_ACTION_PROGRESS, progressReport);
        //response.put(additionalAttributes);
        objectUnderTest.isActionAlreadyRunning(actioName, response);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }

    @Test
    public void testPrepareResponseForRestore() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "restorebackup");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        List<String> moAttributes = new ArrayList<>();
        moAttributes.add("testprogress-report");
        additionalAttributes.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);
        additionalAttributes.put(ShmMediationConstants.ACTIVITY_NAME, "restorebackup");

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(attributeParser.getAttributeMap()).thenReturn(additionalAttributes);

        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(modelTransformer.transformToDpsByData(Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(Map.class),Mockito.any(Boolean.class),Mockito.any(String.class))).thenReturn(newTransformedAttributes);

        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
        final String actioName = "create-backup";
        response.put(ShmMediationConstants.ASYNC_ACTION_PROGRESS, progressReport);
        //response.put(additionalAttributes);
        objectUnderTest.isActionAlreadyRunning(actioName, response);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }

    @Test
    public void testPrepareResponseForDefaultCase() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "backup");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        List<String> moAttributes = new ArrayList<>();
        moAttributes.add("testprogress-report");
        additionalAttributes.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);
        additionalAttributes.put(ShmMediationConstants.ACTIVITY_NAME, "backup");

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(attributeParser.getAttributeMap()).thenReturn(additionalAttributes);

        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(modelTransformer.transformToDpsByData(Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(Map.class),Mockito.any(Boolean.class),Mockito.any(String.class))).thenReturn(newTransformedAttributes);

        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
        final String actioName = "create-backup";
        response.put(ShmMediationConstants.ASYNC_ACTION_PROGRESS, progressReport);
        //response.put(additionalAttributes);
        objectUnderTest.isActionAlreadyRunning(actioName, response);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }


    @Test
    public void testPrepareResponseForNotNullForUpMo() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "create-backup");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        List<String> moAttributes = new ArrayList<>();
        moAttributes.add("testprogress-report");
        additionalAttributes.put(ShmMediationConstants.MO_ATTRIBUTES, moAttributes);
        //additionalAttributes.put(expectedFilter, additionalAttributes)
        //final String activityName = additionalAttributes.get(ShmMediationConstants.ACTIVITY_NAME);

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(attributeParser.getAttributeMap()).thenReturn(additionalAttributes);
        //when(dpsHelperMock.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);

        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(modelTransformer.transformToDpsByData(Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(Map.class),Mockito.any(Boolean.class),Mockito.any(String.class))).thenReturn(newTransformedAttributes);

        final List<String> attributes = new ArrayList<String>();
        attributes.add("reportProgress");
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
        final String actioName = "create-backup";
        response.put(ShmMediationConstants.UP_MO_REPORT_PROGRESS, progressReport);
        //response.put(additionalAttributes);
        objectUnderTest.isActionAlreadyRunning(actioName, response);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }

    @Test(expected=EventHandlerException.class)
    public void testReadAttributesForNull() {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");

        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        final String filter = null;
        final NetconfManager netconfManager = null;
        objectUnderTest.readAttributes(netconfManager, filter, headers, moFdn);
    }

    @Test(expected=EventHandlerException.class)
    public void testReadAttributesForNotNull() throws NetconfManagerException, DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");

        when(modelServiceHelper.getPrimaryKeyAttribute(moType, moNameSpace, moVersion)).thenReturn(expectedAttributes);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(readFilterBuilder.prepareFilter(fdn, expectedAttributes, headers)).thenReturn(expectedFilter);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");

        final List<String> attributes = new ArrayList<String>();
        attributes.add("Progress-report");
        final String filter = getTestFilter();
        final Filter subtreeFilter = new SubTreeFilter(filter);
        when(netconfManagerMock.getStatus()).thenReturn(NetconfConnectionStatus.NOT_CONNECTED);
        when(netconfManagerMock.get(Mockito.any(Filter.class))).thenReturn(netconfResponseMock);
        when(netconfResponseMock.getData()).thenReturn(getTestFilter());
        //when(objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes)).thenReturn(newTransformedAttributes);
        //final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);

        objectUnderTest.readAttributes(netconfManagerMock, filter, headers, moFdn);
        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }

    @Test
    public void testPrepareResponseForNodeHealth() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "NODE_HEALTH_CHECK");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        //final Map<String, Object> headers = new HashMap<>();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);

        final Map<String,Object> newTransformedAttributes = new HashMap<>();
        newTransformedAttributes.put("moNamespace","urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(modelTransformer.transformToDpsByData(Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(String.class),Mockito.any(Map.class),Mockito.any(Boolean.class),Mockito.any(String.class))).thenReturn(newTransformedAttributes);

        final List<String> attributes = new ArrayList<String>();
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);
        final String actioName = "NODE_HEALTH_CHECK";

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }

    @Test
    public void testPrepareResponseForNodeHealthTry() throws DataTypeConversionException {
        mockHeader();
        mockEventAttributes();
        Map<String, Object> hcJobAndChildMosData = null;
        final Map<String, Object> progressReport = new HashMap<>();
        progressReport.put(ShmMediationConstants.ACTION_NAME, "NODE_HEALTH_CHECK");
        final String moFdn = "";
        final String expectedFilter = getTestFilter();
        final List<String> expectedAttributes = new ArrayList<String>();
        expectedAttributes.add("majorType");
        expectedAttributes.add("minorType");
        final List<String> attributes = new ArrayList<String>();
        final Map<String, Object> response = objectUnderTest.prepareResponse(netconfResponseMock, headers, moFdn, attributes);

        Assert.assertNotEquals("Result filter is not matching with expected filter", expectedFilter, fdn);
        Assert.assertNotEquals("Result attributes are  matching with expected attributes", expectedAttributes, attributes);
    }
}

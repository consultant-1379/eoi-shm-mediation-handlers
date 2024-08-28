/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.shm.eoi.rpc;

import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.when;

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
import org.mockito.internal.matchers.Any;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.mediation.cba.handlers.ejb.DpsHelper;
import com.ericsson.oss.mediation.cba.handlers.read.ModelServiceHelper;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.mediation.shm.eoi.rpc.ReadFilterBuilderTest;

/**
 * Junit test class for {@link ReadFilterBuilder}.
 *
 * @author xnalman
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadFilterBuilderTest {
    @InjectMocks
    private ReadFilterBuilder objectUnderTest;

    @Mock
    private DataPersistenceService dataPersistenceService;
    @Mock
    private DataBucket dataBucketMock;
    @Mock
    private ModelServiceHelper modelServiceHelperMock;

    @Mock
    private ManagedObject managedElementMock, systemFunctionsMock, fmMock, fmAlarmModelMock, fmAlarmTypeMock, plmnMock;

    private static final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
    private static final String MULTI_KEY_DELIMITER = "..";

    private final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
            + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
            + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadFilterBuilder#prepareFilter(java.lang.String, java.util.List, java.util.Map)}.
     */
    @Test
    public void testPrepareFilterForSuccess() {
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();

        when(modelServiceHelperMock.getPrimaryKeyAttribute(matches("ManagedElement"), matches("urn:rdns:com:ericsson:oammodel:ericsson-brm"), matches("3.0.1"))).thenReturn(Arrays.asList(new String[] { "managedElementId" }));
        when(modelServiceHelperMock.getPrimaryKeyAttribute(matches("Fm"), matches("urn:rdns:com:ericsson:oammodel:ericsson-fm"), matches("3.0.0"))).thenReturn(Arrays.asList(new String[] { "fmId" }));
        when(modelServiceHelperMock.getPrimaryKeyAttribute(matches("FmAlarmModel"), matches("urn:rdns:com:ericsson:oammodel:ericsson-fm"), matches("3.0.0"))).thenReturn(Arrays.asList(new String[] { "fmAlarmModelId" }));
        when(modelServiceHelperMock.getPrimaryKeyAttribute(matches("FmAlarmType"), matches("urn:rdns:com:ericsson:oammodel:ericsson-fm"), matches("3.0.0"))).thenReturn(Arrays.asList(new String[] { "fmAlarmTypeId" }));

        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", expectedFilter, filter);
    }
    @Test
    public void testPrepareFilterForIf() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system,progress-report=0";
        final String moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        final String moType = "UpgradePackage";
        final String moVersion = "3.0.1";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.TYPE, moType);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        headers.put(ShmMediationConstants.UPGRADEPACKAGE, moType);
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("UpgradePackage");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("UpgradePackage");
        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.shm.ecim.rpc.ReadFilterBuilder#prepareFilter(java.lang.String, java.util.List, java.util.Map)}.
     */
    @Test
    public void testPrepareFilterForSuccessWithSubNetwork() {
        final String startingString = "SubNetwork=NETSimW,";
        final String fdn = startingString + ReadFilterBuilderTest.fdn;
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dpsHelperMock.getManagedObject(matches("brm=1"))).thenReturn(managedElementMock);
        //MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,brm=1,backup-manager=log-system
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");

        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", filter, expectedFilter);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.shm.eoi.rpc.ReadFilterBuilder#prepareFilter(java.lang.String, java.util.List, java.util.Map)}.
     */
    @Test
    public void testPrepareFilterForSuccessWithMeContext() {
        final String startingString = "MeContext=CORE126EPG001,";
        final String fdn = startingString + ReadFilterBuilderTest.fdn;
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", filter, expectedFilter);
    }

    @Test
    public void testPrepareFilterForSuccessWithBrmBackup() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
        final String moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        final String moType = "backup-manager";
        final String moVersion = "3.0.1";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.TYPE, moType);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", filter, expectedFilter);
    }

    @Test
    public void testPrepareFilterForSuccessWithBrmBackupAndEmptyNameSpace() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
        final String moType = "backup-manager";
        final String moVersion = "3.0.1";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.NAMESPACE, "");
        headers.put(ShmMediationConstants.TYPE, moType);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();

        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", filter, expectedFilter);
    }

    @Test
    public void testPrepareFilterForSuccessWithUpgradePackage() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
        final String moNameSpace = "urn:rdns:com:ericsson:oammodel:ericsson-brm";
        final String moType = "backup-manager";
        final String moVersion = "3.0.1";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.TYPE, moType);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", filter, expectedFilter);
    }

    @Test(expected = EventHandlerException.class)
    public void testPrepareFilterWithOnePrimaryKeyAsNull() {
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();

          when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm"); //
          when(managedElementMock.getVersion()).thenReturn("3.0.1"); //
          when(managedElementMock.getType()).thenReturn("backup-manager");
          //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
          when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
          when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);

        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        when(modelServiceHelperMock.getPrimaryKeyAttribute(matches("backup-manager"), matches("urn:rdns:com:ericsson:oammodel:ericsson-brm"),
                matches("3.0.1"))).thenReturn(null);
        objectUnderTest.prepareFilter(fdn, attributesList, headers);
    }

    @Test
    public void testPrepareFilterForMultiplePrimaryKey() {
        final String expectedMultiKeyFilter =  "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        setUpData();
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");

        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);

        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", expectedMultiKeyFilter, filter);
    }

    @Test
    public void testPrepareFilterSuccessWithMultiKeyValues() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");

        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);
        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", expectedFilter, filter);
    }

    @Test
    public void testPrepareFilterSuccessWithMultiplePrimaryAndMultiKeyValues() {
        final String fdn = "MeContext=CORE126EPG001,ManagedElement=CORE126EPG001,fm=1,alarm-model=EpgNode,alarm-type=FunctionNotAvailableAlarmType";
        final String expectedFilter = "<fm xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" + "<alarm-model xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">" 
                + "<alarm-type xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-brm\">"
                + "<majorType/>" + "<minorType/>" + "</alarm-type>" + "</alarm-model>" + "</fm>";
        final List<String> attributesList = new ArrayList<String>();
        final Map<String, Object> headers = new HashMap<String, Object>();
        attributesList.add("majorType");
        attributesList.add("minorType");

        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");

        final String filter = objectUnderTest.prepareFilter(fdn, attributesList, headers);
        Assert.assertNotNull("Filter should not be null", filter);
        Assert.assertEquals("Result filter is not matching with expected filter", expectedFilter, filter);
    }

    private void setUpData() {
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);

        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
        //when(dataPersistenceService.getManagedObject(Mockito.any(String.class))).thenReturn(managedElementMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findMoByFdn(Matchers.any(String.class))).thenReturn(managedElementMock);
        when(managedElementMock.getNamespace()).thenReturn("urn:rdns:com:ericsson:oammodel:ericsson-brm");
        when(managedElementMock.getVersion()).thenReturn("3.0.1");
        when(managedElementMock.getType()).thenReturn("backup-manager");
    }

}

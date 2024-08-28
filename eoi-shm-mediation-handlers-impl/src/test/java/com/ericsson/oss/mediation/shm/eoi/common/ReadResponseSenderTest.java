/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.shm.eoi.common;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadResponse;

/**
 * Junit test class for {@link ReadResponseSender}.
 *
 * @author xarirud
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadResponseSenderTest {

    @InjectMocks
    private ReadResponseSender objectUnderTest;

    @Mock
    private EventSender<MOReadResponse> eventSenderMock;

    @Mock
    private ChannelLocator channelLocator;

    @Mock
    private Channel channelMock;

    final String ERROR_MESSAGE = "ERROR_MESSAGE";

    private Map<String, Object> getMockedHeader() {
        final String moNameSpace = "BrmBackup";//"RcsBrM","RcsSwM"
        final String moType = "BrmBackup";
        final String moVersion = "2.3.0";
        final String fdn = "SubNetwork=NETSimW,ManagedElement=LTE01dg2ERBS00001,SystemFunctions=1,BrM=1,BrmBackupManager=1,BrmBackup=1";
        //fdn = "SubNetwork=LTE01dg2ERBS00015,MeContext=LTE01dg2ERBS00015,ManagedElement=LTE01dg2ERBS00015,SystemFunctions=1,SwM=1,UpgradePackage=17ARadioNodePackage1";
        final Map<String, Object> headers = new HashMap<String, Object>();
        final Map<String, Object> additionalInfo = new HashMap<String, Object>();
        additionalInfo.put(ShmMediationConstants.TYPE, moType);
        headers.put(ShmMediationConstants.ADDITIONAL_INFO, additionalInfo);
        headers.put(ShmMediationConstants.NAMESPACE, moNameSpace);
        headers.put(ShmMediationConstants.VERSION, moVersion);
        headers.put(ShmMediationConstants.FDN, fdn);
        headers.put(ShmMediationConstants.ACTIVITY_JOB_ID, 123L);
        return headers;
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendResponse(java.util.Map, java.util.Map)}.
     */
    @Test
    public void testSendResponse_Success() {
        final Map<String, Object> headers = getMockedHeader();
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        objectUnderTest.sendResponse(headers, attributesMap);
        verify(eventSenderMock, times(1)).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendResponse_Success_toChannel() {
        final Map<String, Object> headers = getMockedHeader();
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        ((Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO)).put(ShmMediationConstants.POLLING_CALLBACK_QUEUE, "queue:/jms/nhcQueue");
        Mockito.when(channelLocator.lookupChannel("queue:/jms/nhcQueue")).thenReturn(channelMock);

        objectUnderTest.sendResponse(headers, attributesMap);
        verify(eventSenderMock, never()).send(any(MOReadResponse.class));
        verify(channelLocator, times(1)).lookupChannel("queue:/jms/nhcQueue");
        verify(channelMock, times(1)).send(any(MOReadResponse.class));
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendResponse(java.util.Map, java.util.Map)}.
     */
    @Test
    public void testSendResponse_FailureWithNullHeader() {
        objectUnderTest.sendResponse(null, null);
        verify(eventSenderMock, never()).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendResponse(java.util.Map, java.util.Map)}.
     */
    @Test
    public void testSendResponse_FailureDuringSendResponse() {
        final Map<String, Object> headers = getMockedHeader();
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        doThrow(new IllegalStateException()).when(eventSenderMock).send(any(MOReadResponse.class));
        objectUnderTest.sendResponse(headers, attributesMap);
        verify(eventSenderMock, times(1)).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendErrorResponse(java.util.Map, java.lang.String)}.
     */
    @Test
    public void testSendErrorResponse_Success() {
        final Map<String, Object> headers = getMockedHeader();
        objectUnderTest.sendErrorResponse(headers, ERROR_MESSAGE);
        verify(eventSenderMock, times(1)).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendErrorResponse(java.util.Map, java.lang.String)}.
     */
    @Test
    public void testSendErrorResponse_FailureWithNullHeader() {
        final Map<String, Object> headers = null;
        objectUnderTest.sendErrorResponse(headers, ERROR_MESSAGE);
        verify(eventSenderMock, never()).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendErrorResponse(java.util.Map, java.lang.String)}.
     */
    @Test
    public void testSendErrorResponse_FailureWithEmptyHeader() {
        final Map<String, Object> headers = new HashMap<String, Object>();
        objectUnderTest.sendErrorResponse(headers, ERROR_MESSAGE);
        verify(eventSenderMock, never()).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.ReadResponseSender#sendErrorResponse(java.util.Map, java.lang.String)}.
     */
    @Test
    public void testSendErrorResponse_FailureDuringSendResponse() {
        final Map<String, Object> headers = getMockedHeader();
        doThrow(new IllegalStateException()).when(eventSenderMock).send(any(MOReadResponse.class));
        objectUnderTest.sendErrorResponse(headers, ERROR_MESSAGE);
        verify(eventSenderMock, times(1)).send(any(MOReadResponse.class));
        verify(channelLocator, never()).lookupChannel(anyString());
    }

}

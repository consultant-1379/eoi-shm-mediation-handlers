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
package com.ericsson.oss.mediation.shm.eoi.handler.api;

import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerConstants.HandlerAttributeKey.NETCONF_SESSION_OPERATIONS_STATUS;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;

/**
 * Junit test class for {@link ShmEventInputHandler}.
 *
 * @author xnalman
 */
@RunWith(MockitoJUnitRunner.class)
public class ShmEventInputHandlerTest {

    private ShmEventInputHandler objectUnderTest = Mockito.mock(ShmEventInputHandler.class, Mockito.CALLS_REAL_METHODS);

    @Mock
    private EventHandlerContext eventHandlerContextMock;
    @Mock
    private ComponentEvent componentEventMock;
    @Mock
    private Configuration configMock;
    @Mock
    private ShmEventInputHandler shmEventInputHandler;
    @Mock
    private NetconfSessionOperationsStatus netconfSessionOperationsStatus;

    private ShmEventInputHandler getTestShmEventInputHandler() {
        final ShmEventInputHandler testObject = new ShmEventInputHandler() {

            private Logger logger = LoggerFactory.getLogger(ShmEventInputHandler.class);

            @Override
            public ComponentEvent onEvent(ComponentEvent inputEvent) {
                return null;
            }

            @Override
            protected void handleError(final String errorMessage, final ComponentEvent inputEvent, final long startTime) {

            }

            @Override
            protected Logger getLogger() {
                return logger;
            }

            @Override
            protected String getHandlerName() {
                return ShmEventInputHandler.class.getName();
            }

            @Override
            protected String getHandlerCanonicalName() {
                return ShmEventInputHandler.class.getCanonicalName();
            }
        };
        return testObject;
    }

    private void mockEventAttributes() {
        final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.NONE);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#getLogger()}.
     */
    @Test
    public void testGetLogger() {
        objectUnderTest = getTestShmEventInputHandler();
        objectUnderTest.getLogger();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#handleError(java.lang.String, com.ericsson.oss.itpf.common.event.ComponentEvent, long)}.
     */
    @Test
    public void testHandleError() {
        final Map<String, Object> headers = new HashMap<String, Object>();
        when(componentEventMock.getHeaders()).thenReturn(headers);
        objectUnderTest = getTestShmEventInputHandler();
        objectUnderTest.handleError("TEST_ERROR_MESSAGE", componentEventMock, System.currentTimeMillis());
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInit() {
        final Map<String, Object> eventAttributes = new HashMap<String, Object>();
        objectUnderTest = getTestShmEventInputHandler();
        when(eventHandlerContextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        when(configMock.getAllProperties()).thenReturn(eventAttributes);
        objectUnderTest.init(eventHandlerContextMock);
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#init(com.ericsson.oss.itpf.common.event.handler.EventHandlerContext)}.
     */
    @Test
    public void testInitFailureWithException() {
        objectUnderTest = getTestShmEventInputHandler();
        when(eventHandlerContextMock.getEventHandlerConfiguration()).thenReturn(configMock);
        Mockito.doThrow(new NullPointerException()).when(configMock).getAllProperties();
        objectUnderTest.init(eventHandlerContextMock);
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#destroy()}.
     */
    @Test
    public void testDestroy() {
        objectUnderTest = getTestShmEventInputHandler();
        objectUnderTest.destroy();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.handler.api.ShmEventInputHandler#putOperationStatusInHeader(com.ericsson.oss.itpf.common.event.ComponentEvent, long)}.
     */
    @Test
    public void testPutOperationStatusInHeader() {
        final Map<String, Object> headers = new HashMap<String, Object>();
        when(componentEventMock.getHeaders()).thenReturn(headers);
        objectUnderTest.putOperationStatusInHeader(componentEventMock, System.currentTimeMillis(), NetconfSessionOperation.GET);
    }
    @Test
    public void testIsToBeSkipped() {
        mockEventAttributes();
        final Map<String, Object> headers = new HashMap<String, Object>();
        //final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.SKIP_ALL);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
        //final NetconfSessionOperationStatus operationStatus = netconfSessionOperationsStatus.getOperationStatus(NetconfSessionOperation.GET);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(netconfSessionOperationsStatus.addOperationStatus(operationStatus)).thenReturn(netconfSessionOperationsStatus);
        objectUnderTest.isToBeSkipped(operationStatus);
    }
    @Test
    public void testIsToBeSkippedForNetconfOperation() {
        mockEventAttributes();
        final Map<String, Object> headers = new HashMap<String, Object>();
        //final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        final NetconfSessionOperationStatus operationStatus = new NetconfSessionOperationStatus(NetconfSessionOperationErrorCode.OPERATION_FAILED);
        netconfSessionOperationsStatus.addOperationStatus(operationStatus);
        //final NetconfSessionOperationStatus operationStatus = netconfSessionOperationsStatus.getOperationStatus(NetconfSessionOperation.GET);
        when(componentEventMock.getHeaders()).thenReturn(headers);
        when(netconfSessionOperationsStatus.addOperationStatus(operationStatus)).thenReturn(netconfSessionOperationsStatus);
        objectUnderTest.isToBeSkipped(operationStatus);
    }

}

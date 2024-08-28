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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.cba.handlers.utility.InstrumentationHelper;
import com.ericsson.oss.mediation.cba.handlers.utility.SyncConstant;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.shm.eoi.common.EoiReadRequestService;
import com.ericsson.oss.mediation.shm.eoi.common.ReadResponseSender;
import com.ericsson.oss.mediation.shm.eoi.common.SubTreeFilter;
import com.ericsson.oss.mediation.shm.eoi.handler.api.ShmEventInputHandler;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconManagerConstants;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

/**
 * This class is used for node attributes read operation through netconf and put
 * the result in Queue. It builds netconf filter request, execute it, parse
 * response and returns result.
 *
 * @version 1.0.1
 * @author xnalman
 */
@EventHandler
public class AttributesReaderHandler extends ShmEventInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributesReaderHandler.class);

    private NetconfManager netconfManager;

    @Inject
    private InstrumentationHelper instrumentationHelper;

    @Inject
    private ReadResponseSender readResponseSender;

    @Inject
    private EoiReadRequestService eoiReadRequestService;

    /**
     * As this handler will be invoked after netconf session, we can't throw
     * {@link EventHandlerException}. After netconf related handlers are
     * invoked, netconf resources must be closed to avoid resource leakage.
     * Hence it should not be thrown in such cases.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {
        long startTime = System.currentTimeMillis();
        try {
            final Map<String, Object> headers = inputEvent.getHeaders();
            LOGGER.debug("Extracted header data: [{}].", headers);
            final boolean isNetconfSessionOperationFailed = NetconfSessionMediationHandlerHelper.isNetconfSessionOperationFailed(headers);
            boolean isToBeSkipped = false;

            final NetconfSessionOperationsStatus netconfSessionOperationsStatus = NetconfSessionMediationHandlerHelper.getOperationsStatus(headers);
            final boolean anyOperationInError = netconfSessionOperationsStatus.isAnyOperationInError();

            if (netconfSessionOperationsStatus.getOperationStatus(NetconfSessionOperation.GET) != null) {
                final NetconfSessionOperationStatus operationStatus = netconfSessionOperationsStatus.getOperationStatus(NetconfSessionOperation.GET);
                isToBeSkipped = isToBeSkipped(operationStatus);
            }

            if (isNetconfSessionOperationFailed || anyOperationInError || isToBeSkipped) {
                final String errorReason = NetconfSessionMediationHandlerHelper.getOperationsStatus(headers).getFirstOperationInErrorStatus()
                        .getNetconfSessionOperationErrorCode().getErrorMessage();
                final String errorMessage = (errorReason == null) ? ShmMediationConstants.NETCONF_OPERATION_FAILED
                        : (ShmMediationConstants.NETCONF_OPERATION_FAILED + " and "
                                + String.format(ShmMediationConstants.FAILURE_REASON, errorReason));
                handleError(errorMessage, inputEvent, startTime);
                return new MediationComponentEvent(inputEvent.getHeaders(), errorMessage);
            }

            this.netconfManager = getNetconfManager(headers);
            final String filter = (String) headers.get(ShmMediationConstants.FILTER);
            final String moFdn = (String) headers.get(ShmMediationConstants.FDN);

            startTime = System.currentTimeMillis();
            final NetconfResponse netconfFilteredResponse = getFilteredNetconfResponse(filter);
            validateNetconfResponse(netconfFilteredResponse, moFdn);
            final List<String> requestAttributes = (List<String>) headers.get(ShmMediationConstants.MO_ATTRIBUTES);
            LOGGER.debug("Value of netconfFilteredResponse {}", netconfFilteredResponse);
            LOGGER.debug("Value of moFdn {}", moFdn);
            final Map<String, Object> responseAttributes = eoiReadRequestService.prepareResponse(netconfFilteredResponse, headers, moFdn,
                    requestAttributes);
            readResponseSender.sendResponse(headers, responseAttributes);
            recordResponseTime(startTime, moFdn);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in {}.onEvent method, reason is : {}", getHandlerName(), ex);
            handleGenericError(ex.getMessage(), inputEvent, startTime);
            if (inputEvent.getHeaders() != null) {
                return new MediationComponentEvent(inputEvent.getHeaders(), ex);
            }
        }
        return inputEvent;
    }

    private void handleGenericError(final String errorMessage, final ComponentEvent inputEvent, final long startTime) {
        LOGGER.error("Exception occurred in AttributesReaderHandler.onEvent method, reason is : {}", errorMessage);
        if (inputEvent == null || inputEvent.getHeaders() == null || inputEvent.getHeaders().isEmpty()) {
            LOGGER.error("Unable to send error response back to SHM as ComponentEvent is null or ComponentEvent header is null or empty.");
        } else {
            handleError(errorMessage, inputEvent, startTime);
        }
    }

    private NetconfManager getNetconfManager(final Map<String, Object> headers) {
        final NetconfManager netconfManagerInstance = (NetconfManager) headers.get(NetconManagerConstants.NETCONF_MANAGER_ATTR);
        LOGGER.trace("In AttributesReaderHandler.getNetconfManager, netconfManagerInstance = {}", netconfManagerInstance);
        if (netconfManagerInstance == null) {
            final String errorMessage = String.format("Unable to find %s by key %s in %s", NetconfManager.class.getName(),
                    NetconManagerConstants.NETCONF_MANAGER_ATTR, getHandlerCanonicalName());
            LOGGER.error(errorMessage);
            throw new EventHandlerException(errorMessage);
        }
        return netconfManagerInstance;
    }

    private NetconfResponse getFilteredNetconfResponse(final String filter) {
        if (filter == null || filter.isEmpty()) {
            throw new EventHandlerException("Filter is empty when fetching NetconfResponse.");
        }
        NetconfResponse response = null;
        final Filter subtreeFilter = new SubTreeFilter(filter);
        try {
            LOGGER.debug("NetconfManager Status: {}", netconfManager.getStatus());
            if (netconfManager.getStatus() != NetconfConnectionStatus.CONNECTED) {
                netconfManager.connect();
            }
            instrumentationHelper.incrementRequestCount();
            response = netconfManager.get(subtreeFilter);
            instrumentationHelper.incrementResponseCount();
            LOGGER.debug("Netconf response returned from node is: [{}] for RPC request : {}", response, filter);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in while processing for NetconfRequest: {}. Exception is : {}", filter, ex);
            throw new EventHandlerException(ex);
        }
        return response;
    }

    private void validateNetconfResponse(final NetconfResponse response, final String moFdn) {
        if (response == null) {
            throw new EventHandlerException("NetconfResponse is null for MO: " + moFdn);
        }
        if (response.isError()) {
            final String netconfErrorMessage = response.getErrorMessage();
            final String errorCodeMessage = String.format(ShmMediationConstants.RESPONSE_ERROR_CODE, response.getErrorCode());
            final String errorMessage = (netconfErrorMessage != null && !netconfErrorMessage.isEmpty()) ? (netconfErrorMessage + errorCodeMessage)
                    : (ShmMediationConstants.NETCONF_OPERATION_FAILED + errorCodeMessage);
            LOGGER.error("{} for MO: {}", errorMessage, moFdn);
            if (response.getErrorCode() == SyncConstant.TIMEOUT_ERROR) {
                LOGGER.error("Timeout during the GET netconf operation for MO: {} in AttributesReaderHandler.validateNetconfResponse.", moFdn);
            }
            throw new EventHandlerException(errorMessage);
        }
        if (response.getData() == null || response.getData().isEmpty()) {
            throw new EventHandlerException("Netconf body message is null or empty for MO: " + moFdn);
        }
    }

    private void recordResponseTime(final long startTime, final String moFdn) {
        final long responseTimeNetconf = System.currentTimeMillis() - startTime;
        instrumentationHelper.addReqCrudProcessTime(responseTimeNetconf / 2);
        instrumentationHelper.addResCrudProcessTime(responseTimeNetconf / 2);
        LOGGER.trace("Time taken to receive response from Netconf : {} ms for MO: {}", responseTimeNetconf, moFdn);
    }

    @Override
    public void handleError(final String errorMessage, final ComponentEvent inputEvent, final long startTime) {
        LOGGER.error("Error occurred in {} as : {}", getHandlerName(), errorMessage);
        readResponseSender.sendErrorResponse(inputEvent.getHeaders(), errorMessage);
        putOperationStatusInHeader(inputEvent, startTime, NetconfSessionOperation.GET);
    }

    @Override
    protected String getHandlerCanonicalName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void destroy() {
        super.destroy();
        this.netconfManager = null;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getHandlerName() {
        return this.getClass().getName();
    }

}

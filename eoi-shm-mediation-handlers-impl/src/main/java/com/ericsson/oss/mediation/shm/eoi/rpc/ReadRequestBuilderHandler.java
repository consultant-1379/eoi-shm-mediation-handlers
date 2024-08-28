/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.shm.eoi.rpc;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.cba.handlers.read.NetconfFilterGetConstants;
import com.ericsson.oss.mediation.shm.eoi.handler.api.ShmEventInputHandler;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;

/**
 * This handler is used to build a filter which will be used in to get request.for the node on which read is to be performed.
 *
 * @version 1.0.1
 * @author xnalman
 */
@SuppressWarnings("unchecked")
@EventHandler
public class ReadRequestBuilderHandler extends ShmEventInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadRequestBuilderHandler.class);

    @Inject
    private ReadFilterBuilder readFilterBuilder;

    /**
     * As this handler will be invoked before netconf session, we can throw {@link EventHandlerException} which will then be handler through errorFlow. But after netconf related handlers are invoked,
     * netconf resources must be closed to avoid resource leakage. Hence it should not be thrown in such cases.
     */
    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {
        try {
            final Map<String, Object> headers = inputEvent.getHeaders();
            LOGGER.debug("Extracted header data: [{}].", headers);
            final String moFdn = (String) headers.get(ShmMediationConstants.FDN);
            final List<String> attributes = (List<String>) headers.get(ShmMediationConstants.MO_ATTRIBUTES);
            final String filter = readFilterBuilder.prepareFilter(moFdn, attributes, headers);
            LOGGER.debug("RPC request: [{}] for MO FDN: {}", filter, moFdn);
            inputEvent.getHeaders().put(NetconfFilterGetConstants.FILTER, filter);
        } catch (final Exception ex) {
            final String errorMessage = "Unable to build RPC read request, reason is : {}";
            LOGGER.error(errorMessage, ex.getMessage());
            throw new EventHandlerException(errorMessage + ex.getMessage());
        }
        return inputEvent;
    }

    @Override
    protected String getHandlerName() {
        return this.getClass().getName();
    }

    @Override
    protected String getHandlerCanonicalName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void handleError(final String errorMessage, final ComponentEvent inputEvent, final long startTime) {
        // Do nothing because this handler errors should be handled by using ShmEoiErrorHandler through errorFlow.
    }

}

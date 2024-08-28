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

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.shm.ecim.common.MoActionResponseSender;
import com.ericsson.oss.mediation.shm.eoi.common.ReadResponseSender;
import com.ericsson.oss.mediation.shm.eoi.handler.api.ShmEventInputHandler;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;

/**
 * This class is used for handling errors through errorFlow.
 *
 * @version 1.0.0
 * @author xnalman
 */
@EventHandler
public class ShmEoiErrorHandler extends ShmEventInputHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShmEoiErrorHandler.class);

    @Inject
    private ReadResponseSender readResponseSender;

    @Inject
    private MoActionResponseSender actionResponseSender;

    @SuppressWarnings("unchecked")
    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {
        try {
            final Map<String, Object> headers = inputEvent.getHeaders();
            LOGGER.debug("Extracted header data: [{}].", headers);
            LOGGER.error("{} is : {}", ShmMediationConstants.ORIGINAL_FLOW_ERROR, headers.get(ShmMediationConstants.ORIGINAL_FLOW_ERROR));
            final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
            final String operationType = (String) additionalAttributes.get(ShmMediationConstants.OPERATION_TYPE);
            if (ShmMediationConstants.ACTION.equals(operationType)) {
                actionResponseSender.sendErrorResponse(headers, headers.get(ShmMediationConstants.ORIGINAL_FLOW_ERROR).toString());
            } else {
                readResponseSender.sendErrorResponse(headers, headers.get(ShmMediationConstants.ORIGINAL_FLOW_ERROR).toString());
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while handling error scenario : ", ex);
        }
        return inputEvent;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
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
    protected void handleError(final String errorMessage, final ComponentEvent inputEvent, final long startTime) {
        // Do nothing because this handler is used as error handler and if any error occurs here then it needs to be handled separately.
    }

}

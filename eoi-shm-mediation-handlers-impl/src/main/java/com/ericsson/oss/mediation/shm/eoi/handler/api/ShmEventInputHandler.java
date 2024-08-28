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

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.TypedEventInputHandler;
import com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;

/**
 * This interface can be used for handlers.
 *
 * @version 1.0.0
 * @author xnalman
 */
@SuppressWarnings("deprecation")
public abstract class ShmEventInputHandler implements TypedEventInputHandler {

    protected abstract Logger getLogger();

    protected abstract void handleError(final String errorMessage, final ComponentEvent inputEvent, final long startTime);

    protected abstract String getHandlerName();

    protected abstract String getHandlerCanonicalName();

    @Override
    public void init(final EventHandlerContext ctx) {
        try {
            getLogger().debug("In {}.init and extracted context properties as : {}", getHandlerName(), ctx.getEventHandlerConfiguration().getAllProperties());
        } catch (final Exception ex) {
            getLogger().error("Exception occurred while getting context properties, reason is : {}", ex.getMessage());
        }
    }

    @Override
    public void destroy() {
        getLogger().trace("Entered into {}.destroy method.", getHandlerName());
    }

    protected void putOperationStatusInHeader(final ComponentEvent inputEvent, final long startTime, final NetconfSessionOperation netconfSessionOperation) {
        final NetconfSessionOperationErrorCode netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED;
        NetconfSessionMediationHandlerHelper.putOperationStatusInHeader(
                NetconfSessionMediationHandlerHelper.buildOperationStatus(netconfSessionOperation, netconfSessionOperationErrorCode, startTime, System.currentTimeMillis()), inputEvent.getHeaders());
    }

    protected boolean isToBeSkipped(final NetconfSessionOperationStatus operationStatus) {
        boolean isToBeSkipped = false;
        if (NetconfSessionOperationErrorCode.SKIP_ALL.equals(operationStatus.getNetconfSessionOperationErrorCode())
                || NetconfSessionOperationErrorCode.OPERATION_FAILED.equals(operationStatus.getNetconfSessionOperationErrorCode())
                || NetconfSessionMediationHandlerHelper.isNetconfSessionOperationFailed(NetconfSessionOperation.SESSION_BUILDER, operationStatus)) {
            isToBeSkipped = true;
        }
        return isToBeSkipped;
    }

}

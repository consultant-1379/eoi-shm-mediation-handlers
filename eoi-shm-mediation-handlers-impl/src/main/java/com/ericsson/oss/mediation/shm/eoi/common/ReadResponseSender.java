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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadResponse;

/**
 * This class is used for returning node read response back to SHM for both successful and error cases.
 *
 * @version 1.0.0
 * @author xarirud
 */
public class ReadResponseSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadResponseSender.class);

    @Inject
    private ChannelLocator channelLocator;

    @Inject
    @Modeled
    private EventSender<MOReadResponse> eventSender;

    public void sendResponse(final Map<String, Object> headers, final Map<String, Object> attributesMap) {
        try {
            final MOReadResponse readResponse = prepareResponse(headers);
            readResponse.setMoAttributes(attributesMap);

            post(headers, readResponse);

            LOGGER.debug("Successfully sent response: {} to SHM for moFdn {} .", readResponse.getMoAttributes(), readResponse.getMoFdn());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while sending response back. Reason : ", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void post(final Map<String, Object> headers, final MOReadResponse readResponse) {
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        final String channelURI = (String) additionalAttributes.get(ShmMediationConstants.POLLING_CALLBACK_QUEUE);

        if (channelURI != null && !channelURI.isEmpty()) {
            LOGGER.debug("callbackQueue:{} is mentioned explicitly in MediationTaskRequest.", channelURI);
            final Channel channel = channelLocator.lookupChannel(channelURI);
            channel.send(readResponse);
        } else {
            //As there is no explicit callback queue mentioned in properties, post the mediation response onto the default model channel.
            eventSender.send(readResponse);
        }
    }

    public void sendErrorResponse(final Map<String, Object> headers, final String errorMessage) {
        try {
            if (headers != null && !headers.isEmpty()) {
                final MOReadResponse readResponse = prepareResponse(headers);
                readResponse.setErrorMessage(errorMessage);

                post(headers, readResponse);

                LOGGER.error("Sent error response to SHM as {} for moFdn {} having activityJobId {}.", errorMessage, readResponse.getMoFdn(), readResponse.getActivityJobId());
            } else {
                LOGGER.error("Failed to send response as header is null or empty.");
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while sending response back. Reason : ", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private MOReadResponse prepareResponse(final Map<String, Object> headers) {
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        final long activityJobId = (long) headers.get(ShmMediationConstants.ACTIVITY_JOB_ID);
        final String jobType = (String) additionalAttributes.get(ShmMediationConstants.JOB_TYPE);
        final String platform = (String) additionalAttributes.get(ShmMediationConstants.PLATFORM);
        final String activityName = (String) additionalAttributes.get(ShmMediationConstants.ACTIVITY_NAME);

        final Map<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put(ShmMediationConstants.JOB_TYPE, jobType);
        additionalInformation.put(ShmMediationConstants.PLATFORM, platform);
        additionalInformation.put(ShmMediationConstants.ACTIVITY_NAME, activityName);

        final MOReadResponse readFlowResponse = new MOReadResponse();
        readFlowResponse.setAdditionalInformation(additionalInformation);
        readFlowResponse.setActivityJobId(activityJobId);
        final String moFdn = (String) headers.get(ShmMediationConstants.FDN);
        readFlowResponse.setMoFdn(moFdn);
        return readFlowResponse;
    }

}

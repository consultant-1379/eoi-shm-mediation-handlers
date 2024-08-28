/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.shm.eoi.common;

import java.util.*;

import javax.inject.Inject;

import com.ericsson.oss.mediation.adapter.transformer.converter.DataTypeConversionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.cba.handlers.read.ModelServiceHelper;
import com.ericsson.oss.mediation.cba.handlers.read.NetconfFilterGetConstants;
import com.ericsson.oss.mediation.cba.handlers.read.ReadNonPersistenceAttributeParser;
import com.ericsson.oss.mediation.adapter.transformer.ModelTransformer;
import com.ericsson.oss.mediation.cba.handlers.utility.InstrumentationHelper;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;
import com.ericsson.oss.mediation.shm.eoi.rpc.ReadFilterBuilder;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

public class CommonReadRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReadRequestService.class);

    @Inject
    private ModelServiceHelper modelServiceHelper;

    @Inject
    private ReadFilterBuilder readFilterBuilder;

    @Inject
    private ReadNonPersistenceAttributeParser attributeParser;

    @Inject
    private InstrumentationHelper instrumentationHelper;

    @Inject
    private ModelTransformer modelTransformer;

    public String getReadFilter(final String moFdn, final ComponentEvent inputEvent) {
        String filter = "";
        try {
            final Map<String, Object> headers = inputEvent.getHeaders();
            final List<String> requestAttributes = getRequestAttributesFromActionRequest(headers);
            filter = readFilterBuilder.prepareFilter(moFdn, requestAttributes, headers);
            LOGGER.debug("In CommonReadRequestService.getReadFilter, filter from filterBuilder : {}", filter);
            inputEvent.getHeaders().put(NetconfFilterGetConstants.FILTER, filter);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while building RPC request for read call : {}", ex.getMessage());
        }
        return filter;
    }

    public Map<String, Object> readAttributes(final NetconfManager netconfManager, final String readFilter, final Map<String, Object> headers, final String moFdn) {
        if (readFilter == null || readFilter.isEmpty()) {
            throw new EventHandlerException("Filter is empty when fetching NetconfResponse.");
        }
        Map<String, Object> response = new HashMap<>();
        try {
            final Filter subtreeFilter = new SubTreeFilter(readFilter);
            LOGGER.debug("NetconfManager Status: {}", netconfManager.getStatus());
            if (netconfManager.getStatus() != NetconfConnectionStatus.CONNECTED) {
                netconfManager.connect();
            }
            instrumentationHelper.incrementRequestCount();
            final NetconfResponse netconfResponse = netconfManager.get(subtreeFilter);
            final List<String> readAttributes = getRequestAttributesFromActionRequest(headers);
            response = prepareResponse(netconfResponse, headers, moFdn, readAttributes);
            instrumentationHelper.incrementResponseCount();
            LOGGER.debug("In CommonReadRequestService.getFilteredNetconfResponse, netconfResponse: {}", response);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in while processing for NetconfRequest: {}. Exception is : {}", readFilter, ex);
            throw new EventHandlerException(ex);
        }
        return response;
    }

    public Map<String, Object> prepareResponse(final NetconfResponse netconfFilteredResponse, final Map<String, Object> headers, final String moFdn, final List<String> requestAttributes) throws DataTypeConversionException {
        final String netconfResponseData = netconfFilteredResponse.getData();
        final String moNamespace = (String) headers.get(ShmMediationConstants.NAMESPACE);
        final String moMimVersion = (String) headers.get(ShmMediationConstants.VERSION);
        final Map<String, Object> additionalInfo = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        final String activityName = (String) additionalInfo.get(ShmMediationConstants.ACTIVITY_NAME);
        String moType = " ";
        switch(activityName){
        case ShmMediationConstants.CREATE_BACKUP:
        case ShmMediationConstants.DELETE_BACKUP:
        case ShmMediationConstants.DOWNLOAD_BACKUP:
            moType = "backup-manager$$progress-report";
            break;
        case ShmMediationConstants.UPLOAD_BACKUP:
        case ShmMediationConstants.RESTORE_BACKUP:
            moType = "backup$$progress-report";
            break;
        default:
        LOGGER.warn("Invalid activityName {}:", activityName);
        }

        final List<String> primaryKeyAttributes = modelServiceHelper.getPrimaryKeyAttribute(moType, moNamespace, moMimVersion);
        if (primaryKeyAttributes == null || primaryKeyAttributes.isEmpty()) {
            LOGGER.error("In CommonReadRequestService.prepareAndSendResponse, {}", ShmMediationConstants.NULL_KEY_RECEIVED_MSG);
        }
        if (requestAttributes.isEmpty()) {
            LOGGER.error("In CommonReadRequestService.ShmMediationConstants.NODE_HEALTH_CHECK_JOB_TYPE, {}", ShmMediationConstants.JOB_TYPE);
            return new HashMap<>();
        } else {
            final String multiKeyDelimiter = modelServiceHelper.getMultiKeyDelimiter(moType, moNamespace, moMimVersion);
            final String moName = moFdn.substring(moFdn.lastIndexOf('=') + 1);
            final Map<String, Object> responseAttributes = parseNetconfResponse(requestAttributes, netconfResponseData, primaryKeyAttributes, moName, moType, multiKeyDelimiter);
            LOGGER.info("Response attributes: {} for MO FDN: {}", responseAttributes, moFdn);
            final Map<String, Object> newTransformedAttributes = modelTransformer.transformToDpsByData(moNamespace, moMimVersion, moType, moName, responseAttributes, true,
                    (String) additionalInfo.get(ShmMediationConstants.NE_OSS_PREFIX));
            LOGGER.info("transform called and returned {}", newTransformedAttributes);
            return newTransformedAttributes;
        }
    }

    private Map<String, Object> parseNetconfResponse(final List<String> requestAttributes, final String responseData, final List<String> primaryKeyAttributes, final String moName, final String moType,
            final String multiKeyDelimiter) {
        attributeParser.setFdnType(moType);
        attributeParser.setPrimaryKeyAttributes(primaryKeyAttributes);
        attributeParser.setKeyValue(moName);
        attributeParser.setAttributes(requestAttributes);
        attributeParser.setMultiKeyDelimiter(multiKeyDelimiter);
        attributeParser.parseData(responseData);
        final Map<String, Object> responseAttributes = attributeParser.getAttributeMap();
        if (responseAttributes == null || responseAttributes.isEmpty()) {
            final String errorMessage = "attributesMap from parser is null or empty.";
            LOGGER.error(errorMessage);
            throw new EventHandlerException(errorMessage);
        }
        return responseAttributes;
    }

    public boolean isActionAlreadyRunning(final String actioName, final Map<String, Object> response) {
        final Map<String, Object> progressReport = getActionProgress(response);
        if (progressReport != null && progressReport.get(ShmMediationConstants.ACTION_NAME) != null) {
            final String actionNameFromNode = (String) progressReport.get(ShmMediationConstants.ACTION_NAME);
            if (actionNameFromNode.equalsIgnoreCase(actioName)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> getActionProgress(final Map<String, Object> response) {
        Map<String, Object> actionProgress = new HashMap<>();
        if (response.get(ShmMediationConstants.ASYNC_ACTION_PROGRESS) != null) {
            actionProgress = (Map<String, Object>) response.get(ShmMediationConstants.ASYNC_ACTION_PROGRESS);
        } else if (response.get(ShmMediationConstants.UP_MO_REPORT_PROGRESS) != null) {
            actionProgress = (Map<String, Object>) response.get(ShmMediationConstants.UP_MO_REPORT_PROGRESS);
        }
        return actionProgress;
    }

    public List<String> getRequestAttributesFromActionRequest(final Map<String, Object> headers) {
        List<String> requestAttributes = new ArrayList<>();
        final Map<String, Object> additionalAttributes = (Map<String, Object>) headers.get(ShmMediationConstants.ADDITIONAL_INFO);
        if (additionalAttributes != null && additionalAttributes.get(ShmMediationConstants.MO_ATTRIBUTES) != null) {
            requestAttributes = (List<String>) additionalAttributes.get(ShmMediationConstants.MO_ATTRIBUTES);
        }
        return requestAttributes;
    }
}

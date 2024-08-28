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
package com.ericsson.oss.mediation.shm.eoi.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.cba.handlers.ejb.DpsHelper;
import com.ericsson.oss.mediation.cba.handlers.read.ModelServiceHelper;
import com.ericsson.oss.mediation.cba.handlers.read.NetconfFilterGetConstants;
import com.ericsson.oss.mediation.shm.eoi.handler.constants.ShmMediationConstants;

/**
 * This is a helper EJB for preparing required filter.
 *
 * @version 1.0.0
 * @author xnalman
 */
@Stateless
public class ReadFilterBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadFilterBuilder.class);
    public static final String BACKUP = "backup";

    private static final String XML_CLOSE_TAG = ">";

    private static final String NAME_SPACE_URN = " xmlns=";

    private static final String ELEMENT_END_TAG = "</";

    private static final String ELEMENT_START_TAG = "<";

    private static final String ATTRIBUTE_END_TAG = "/>";
    private static final String EQUAL = ShmMediationConstants.DELIMITER_EQUAL;
    private static final String COMMA = ShmMediationConstants.DELIMITER_COMMA;

    private static final String REGEX_START = "\\";

    private static final String BACKSLASH = "\"";

    private static final String ME_CONTEXT = "MeContext";

    private static final String MANAGED_ELEMENT = "ManagedElement";

    private static final String SUB_NETWORK = "SubNetwork";

    @Inject
    private ModelServiceHelper modelServiceHelper;

    @Inject
    private DpsHelper dpsHelper;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * This method will prepare filter for netconf get request.
     *
     * @param fdn
     *            :FDN of MO
     * @param attributes
     *            :Attribute of MO needs to fetch from Netconf
     * @return {@link String} : Filter request for netconf
     */
    public String prepareFilter(final String fdn, final List<String> attributes, final Map<String, Object> headers) {
        final StringBuilder beginFilterBuilder = new StringBuilder();
        final StringBuilder endFilterBuilder = new StringBuilder();
        final StringBuilder moFdn = new StringBuilder();
        final String[] fdnTokens = fdn.split(COMMA);
        LOGGER.debug("Value of fdnTokens {}", fdnTokens);
        for (int i = 0; i < fdnTokens.length; i++) {
            if (moFdn.length() != 0) {
                moFdn.append(COMMA).append(fdnTokens[i]);
            } else {
                moFdn.append(fdnTokens[i]);
            }

            final String[] fdnTokenMoKeyValuePair = fdnTokens[i].split(EQUAL);
            final String moType = fdnTokenMoKeyValuePair[0];
            final String primaryKeyAttributeValue = fdnTokenMoKeyValuePair[1];
            LOGGER.debug("Value of moType {}, moFdn {}", moType, moFdn);

            if (ME_CONTEXT.equals(moType) || SUB_NETWORK.equals(moType) || MANAGED_ELEMENT.equals(moType)) {
                LOGGER.debug("Do nothing for {} , {} and {} having moType as {}", ME_CONTEXT, SUB_NETWORK, MANAGED_ELEMENT, moType);
            } else {
                final Map<String, String> moDetails = getManagedObjectDetails(moType, moFdn, headers);
                constructBeginFilter(moDetails, beginFilterBuilder, moType, primaryKeyAttributeValue);
                addRequiredAttributes(beginFilterBuilder, attributes, fdnTokens.length, i);
                prepareEndFilter(moType, endFilterBuilder);
            }
        }
        return beginFilterBuilder.toString() + endFilterBuilder.toString();
    }

    private Map<String, String> getManagedObjectDetails(final String moType, final StringBuilder moFdn, final Map<String, Object> headers) {
        String namespace = "";
        String type = "";
        String version = "";
        if (BACKUP.equals(moType) || ShmMediationConstants.UPGRADEPACKAGE.equals(moType)) {
            namespace = (String) headers.get(ShmMediationConstants.NAMESPACE);
            version = (String) headers.get(ShmMediationConstants.VERSION);
            final String fdnFromReq = (String) headers.get(ShmMediationConstants.FDN);
            type = fdnFromReq.substring(fdnFromReq.lastIndexOf(COMMA) + 1, fdnFromReq.lastIndexOf(EQUAL));
        } else {
            final ManagedObject managedElement = dataPersistenceService.getLiveBucket().findMoByFdn(moFdn.toString());
            namespace = managedElement.getNamespace();
            type = managedElement.getType();
            version = managedElement.getVersion();
        }
        final Map<String, String> moDetails = new HashMap<>();
        moDetails.put(ShmMediationConstants.NAMESPACE, namespace);
        moDetails.put(ShmMediationConstants.VERSION, version);
        moDetails.put(ShmMediationConstants.TYPE, type);
        LOGGER.debug("Value of moDetails {}", moDetails);
        return moDetails;
    }

    private void constructBeginFilter(final Map<String, String> moDetails, final StringBuilder beginFilterBuilder, final String moType, final String primaryKeyAttributeValue) {
        final String namespace = moDetails.get(ShmMediationConstants.NAMESPACE);
        final List<String> primaryKeyAttributes = getPrimaryKeyAttributes(namespace, moDetails);
        final String multiKeyDelimiter = getMultiKeyDelimiter(namespace, moDetails);
        LOGGER.debug("namespace: {} - multiKeyDelimiter: {} - list of primaryKeyAttributes: {}", namespace, multiKeyDelimiter, primaryKeyAttributes);

        prepareBeginFilter(beginFilterBuilder, moType, primaryKeyAttributes, primaryKeyAttributeValue, namespace, multiKeyDelimiter);
    }

    private List<String> getPrimaryKeyAttributes(final String namespace, final Map<String, String> moDetails) {
        final String type = moDetails.get(ShmMediationConstants.TYPE);
        final String version = moDetails.get(ShmMediationConstants.VERSION);
        final List<String> primaryKeyAttributes = modelServiceHelper.getPrimaryKeyAttribute(type, namespace, version);
        if (primaryKeyAttributes == null) {
            LOGGER.error("Received Error response {} ", NetconfFilterGetConstants.NULL_KEY_RECEIVED_MSG);
            throw new EventHandlerException(NetconfFilterGetConstants.NULL_KEY_RECEIVED_MSG);
        }
        return primaryKeyAttributes;
    }

    private String getMultiKeyDelimiter(final String namespace, final Map<String, String> moDetails) {
        final String type = moDetails.get(ShmMediationConstants.TYPE);
        final String version = moDetails.get(ShmMediationConstants.VERSION);
        return modelServiceHelper.getMultiKeyDelimiter(type, namespace, version);
    }

    private void addRequiredAttributes(final StringBuilder beginFilterBuilder, final List<String> attributes, final int fdnTokensLength, final int index) {
        if (index == (fdnTokensLength - 1)) {
            // If LastElement of Fdn
            for (final String attributeName : attributes) {
                beginFilterBuilder.append(ELEMENT_START_TAG).append(attributeName).append(ATTRIBUTE_END_TAG);
            }
        }
    }

    private void prepareBeginFilter(final StringBuilder beginFilterBuilder, final String managedObjectName, final List<String> primaryKeyAttributes, final String primaryKeyAttributeValue,
            final String nameSpace, final String multiKeyDelimiter) {
        beginFilterBuilder.append(ELEMENT_START_TAG).append(managedObjectName);
        if (!nameSpace.isEmpty()) {
            beginFilterBuilder.append(NAME_SPACE_URN).append(BACKSLASH + nameSpace + BACKSLASH);
        }
        beginFilterBuilder.append(XML_CLOSE_TAG);

        String[] multiKeyValues = null;
        if ((primaryKeyAttributeValue != null) && (multiKeyDelimiter != null) && primaryKeyAttributeValue.contains(multiKeyDelimiter)) {
            multiKeyValues = primaryKeyAttributeValue.split(REGEX_START + multiKeyDelimiter);
        }

        int multiKeyIndex = 0;
        for (final String primaryKeyAttribute : primaryKeyAttributes) {
            beginFilterBuilder.append(ELEMENT_START_TAG).append(primaryKeyAttribute).append(XML_CLOSE_TAG)
                    .append(((multiKeyValues == null) || (multiKeyValues.length == multiKeyIndex)) ? primaryKeyAttributeValue : multiKeyValues[multiKeyIndex+1]).append(ELEMENT_END_TAG)
                    .append(primaryKeyAttribute).append(XML_CLOSE_TAG);
        }
    }

    private void prepareEndFilter(final String fdnTokenMoName, final StringBuilder endFilterBuilder) {
        final String currentToken = ELEMENT_END_TAG + fdnTokenMoName + XML_CLOSE_TAG;
        endFilterBuilder.insert(0, currentToken);
    }

}

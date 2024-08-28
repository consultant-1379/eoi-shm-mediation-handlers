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
package com.ericsson.oss.mediation.shm.eoi.handler.constants;

/**
 * This class holds constants for this repository.
 *
 * @version 1.0.0
 * @author xnalman
 *
 */
public final class ShmMediationConstants {

    public static final String NAMESPACE = "namespace";
    public static final String ACTIVITY_NAME = "activityName";
    public static final String ADDITIONAL_INFO = "additionalInformation";
    public static final String VERSION = "mimVersion";
    public static final String CREATE_BACKUP = "createbackup";
    public static final String DELETE_BACKUP = "deletebackup";
    public static final String DOWNLOAD_BACKUP = "downloadbackup";
    public static final String UPLOAD_BACKUP = "uploadbackup";
    public static final String RESTORE_BACKUP = "restorebackup";
    public static final String NODE_HEALTH_CHECK_JOB_TYPE = "NODE_HEALTH_CHECK";
    public static final String NULL_KEY_RECEIVED_MSG = "Primary key attribute is null.";
    public static final String NE_OSS_PREFIX = "ossPrefix";
    public static final String JOB_TYPE = "jobType";
    public static final String ACTION_NAME = "actionName";
    public static final String ASYNC_ACTION_PROGRESS = "progressReport";
    public static final String MO_ATTRIBUTES = "moAttributes";
    public static final String ORIGINAL_FLOW_ERROR = "originalFlowException";
    public static final String OPERATION_TYPE = "operationType";
    public static final String ACTION = "action";
    public static final String NETCONF_OPERATION_FAILED = "Netconf operation failed.";
    public static final String FDN = "moFdn";
    public static final String ACTIVITY_JOB_ID = "activityJobId";
    public static final String RESPONSE_ERROR_CODE = " Response Error Code is \"%s\".";
    public static final String FILTER = "filter";
    public static final String FAILURE_REASON = "Failure reason is : \"%s\"";
    public static final String UP_MO_REPORT_PROGRESS = "reportProgress";
    public static final String DELIMITER_COMMA = ",";
    public static final String DELIMITER_EQUAL = "=";
    public static final String UPGRADEPACKAGE = "UpgradePackage";
    public static final String TYPE = "type";
    public static final String POLLING_CALLBACK_QUEUE = "callbackQueue";
    public static final String PLATFORM = "platform";
    public static final String READ = "read";
    public static final String MEDIATION_TASK_REQUEST = "mediationTaskRequest";

    private ShmMediationConstants() {
        throw new IllegalAccessError("It is illegal to initialize or inherit ShmMediationConstants class as it is used only for constants.");
    }

}

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

import com.ericsson.oss.mediation.util.netconf.api.Filter;

/**
 * Helper Subtree filter class, implementing Filter interface.
 */
public class SubTreeFilter implements Filter {

    private static final String TYPE = "subtree";

    private final String filter;

    public SubTreeFilter(final String filter) {
        this.filter = filter;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String asString() {
        return filter;
    }
}
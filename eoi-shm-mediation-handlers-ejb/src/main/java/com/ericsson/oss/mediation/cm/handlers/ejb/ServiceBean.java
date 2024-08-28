/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.cm.handlers.ejb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.mediation.cm.handlers.impl.DateResolverBean;

@Stateless
public class ServiceBean implements ServiceRemote, ServiceLocal {

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    @Inject
    private Logger logger;

    @Inject
    private DateResolverBean dateResolver;

    @Override
    public String resolveDate() {
        logger.debug("In EJB - resolving date");
        final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        return df.format(dateResolver.resolveCurrentDate());
    }

}

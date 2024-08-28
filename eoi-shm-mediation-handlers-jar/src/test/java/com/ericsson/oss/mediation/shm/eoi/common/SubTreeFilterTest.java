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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Junit test class for {@link SubTreeFilter}.
 *
 * @author xnalman
 */
@RunWith(MockitoJUnitRunner.class)
public class SubTreeFilterTest {

    @InjectMocks
    private SubTreeFilter objectUnderTest;

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.SubTreeFilter#SubTreeFilter(java.lang.String)}.
     */
    @Test
    public void testSubTreeFilter() {
        new SubTreeFilter("TEST_STRING");
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.SubTreeFilter#getType()}.
     */
    @Test
    public void testGetType() {
        objectUnderTest.getType();
    }

    /**
     * Test method for {@link com.ericsson.oss.mediation.shm.ecim.common.SubTreeFilter#asString()}.
     */
    @Test
    public void testAsString() {
        objectUnderTest.asString();
    }

}

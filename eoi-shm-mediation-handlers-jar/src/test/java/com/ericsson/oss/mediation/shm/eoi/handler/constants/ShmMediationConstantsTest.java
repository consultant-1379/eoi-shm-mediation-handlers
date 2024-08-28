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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Junit test class for {@link ShmMediationConstants}.
 *
 * @author xnalman
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ShmMediationConstantsTest {

    @Test
    public void test() {
        assertEquals(ShmMediationConstants.ACTIVITY_JOB_ID, ShmMediationConstants.ACTIVITY_JOB_ID);
    }

    @Test(expected = IllegalAccessError.class)
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        Constructor<ShmMediationConstants> constructor = ShmMediationConstants.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (final InvocationTargetException e) {
            throw (IllegalAccessError) e.getTargetException();
        }
    }

}

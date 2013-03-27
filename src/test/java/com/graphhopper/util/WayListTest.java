/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ottavio Campana
 */
public class WayListTest {

    @Test public void testAdd() {
        WayList instance = new WayList();
        instance.add(WayList.CONTINUE_ON_STREET, "First street");
        assertEquals(instance.indication(0), WayList.CONTINUE_ON_STREET);
    }

    @Test public void testReverse() {
        WayList instance = new WayList();
        instance.add(WayList.CONTINUE_ON_STREET, "First street");
        instance.reverse();
        assertEquals(instance.indication(0), WayList.CONTINUE_ON_STREET);
        assertEquals(instance.name(0), "First street");

        instance = new WayList();
        instance.add(WayList.CONTINUE_ON_STREET, "First street");
        instance.add(WayList.TURN_LEFT, "A avenue");
        instance.reverse();
        assertEquals(instance.indication(0), WayList.TURN_LEFT);
        assertEquals(instance.name(0), "A avenue");
        assertEquals(instance.indication(1), WayList.CONTINUE_ON_STREET);
        assertEquals(instance.name(1), "First street");
    }
}

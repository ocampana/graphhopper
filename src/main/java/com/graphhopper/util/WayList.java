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
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Slim list to store several points (without the need for a point object).
 *
 * @author Ottavio Campana
 */
public class WayList {

    public static final int CONTINUE_ON_STREET = 0;
    public static final int TURN_LEFT = 1;
    public static final int TURN_RIGHT = 1;

    private int[] indications;
    private String[] names;
    private int size = 0;

    public WayList() {
        this(10);
    }

    public WayList(int cap) {
        if (cap < 5)
            cap = 5;
        indications = new int[cap];
        names = new String[cap];
    }

    public void set(int index, int indication, String name) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("index has to be smaller than size " + size);

        indications[index] = indication;
        names[index] = name;
    }

    public void add(int indication, String name) {
        int newSize = size + 1;
        if (newSize >= indications.length) {
            int cap = newSize * 3 / 2;
            indications = Arrays.copyOf(indications, cap);
            names = Arrays.copyOf(names, cap);
        }

        indications[size] = indication;
        names[size] = name;
        size = newSize;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int indication(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("Tried to access WayList with too big index! "
                    + "index:" + index + ", size:" + size);
        return indications[index];
    }

    public String name(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("Tried to access WayList with too big index! "
                    + "index:" + index + ", size:" + size);
        return names[index];
    }

    public void reverse() {
        int max = size / 2;
        for (int i = 0; i < max; i++) {
            int swapIndex = size - i - 1;

            int tmp = indications[i];
            indications[i] = indications[swapIndex];
            indications[swapIndex] = tmp;

            String name = names[i];
            names[i] = names[swapIndex];
            names[swapIndex] = name;
        }
    }

    public void clear() {
        size = 0;
    }

    public void trimToSize(int newSize) {
        if (newSize > size)
            throw new IllegalArgumentException("new size needs be smaller than old size");
        size = newSize;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");

            sb.append('(');
            sb.append(indications[i]);
            sb.append(',');
            sb.append(names[i]);
            sb.append(')');
        }
        return sb.toString();
    }

    public WayList trimToSize() {
        // 1 free point is ok
        if (indications.length <= size + 1)
            return this;

        indications = Arrays.copyOf(indications, size);
        names = Arrays.copyOf(names, size);
        return this;
    }
}

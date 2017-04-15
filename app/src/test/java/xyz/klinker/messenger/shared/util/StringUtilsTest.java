/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest extends MessengerSuite {

    @Before
    public void setUp() {
        StringUtils helper = new StringUtils();
    }

    @Test
    public void generate32charHexString() {
        for (int i = 0; i < 10; i++) {
            String hex = StringUtils.generateHexString(32);
            assertEquals(32, hex.length());
        }
    }

    @Test
    public void generate20charHexString() {
        for (int i = 0; i < 10; i++) {
            String hex = StringUtils.generateHexString(20);
            assertEquals(20, hex.length());
        }
    }

    @Test
    public void capitalizeCharacters() {
        assertEquals("Conversation List", StringUtils.titleize("Conversation list"));
        assertEquals("Scheduled Messages", StringUtils.titleize("Scheduled messages"));
        assertEquals("Help & Feedback", StringUtils.titleize("Help & feedback"));
    }

    @Test
    public void split() {
        List<Long> longs = new ArrayList<>();
        longs.add(1L);
        longs.add(2L);
        longs.add(3L);
        longs.add(4L);

        assertEquals("1,2,3,4", StringUtils.join(longs, ","));
        assertEquals("", StringUtils.join(null, ","));
        assertEquals("", StringUtils.join(new ArrayList<Long>(), ","));
    }

    @Test
    public void sqlOr() {
        List<Long> longs = new ArrayList<>();
        longs.add(1L);
        longs.add(2L);
        longs.add(3L);
        longs.add(4L);

        assertEquals("test=1 OR test=2 OR test=3 OR test=4", StringUtils.buildSqlOrStatement("test", longs));

        longs = new ArrayList<>();
        longs.add(1L);

        assertEquals("test=1", StringUtils.buildSqlOrStatement("test", longs));

        longs = new ArrayList<>();
        longs.add(1L);
        longs.add(2L);

        assertEquals("test=1 OR test=2", StringUtils.buildSqlOrStatement("test", longs));


        assertEquals("", StringUtils.buildSqlOrStatement("test", null));
        assertEquals("", StringUtils.buildSqlOrStatement("test", new ArrayList<Long>()));


    }
}
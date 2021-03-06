/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.http.normal;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author zinic
 */
public class QueryStringNormalizerTest {

    private Normalizer<String> queryStringNormalizer;

    @Before
    public void before() {
        final ParameterFilterFactory parameterFilterFactory = new ArrayWhiteListParameterFilterFactory(new String[]{"a", "b", "c", "d"});
        queryStringNormalizer = new QueryStringNormalizer(parameterFilterFactory, true);
    }

    @Test
    public void shouldFilterBadParameters() {
        final String query = "cache-busting=2395819035&a=1";
        final String actual = queryStringNormalizer.normalize(query);

        assertThat("URI normalizer must filter bad query parameters.", actual, not(containsString("cache-busting")));
    }

    @Test
    public void shouldAlphabetizeParameters() {
        final String query = "c=124&a=111&d=4&b=8271";
        final String actual = queryStringNormalizer.normalize(query);

        final String[] queryParamPairs = actual.split("&");

        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=111");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "b=8271");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "c=124");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "d=4");
    }

    @Test
    public void shouldNormalizeContiguousCollections() {
        final String query = "b=4&c=111&a=1&a=2&a=3&d=441";
        final String actual = queryStringNormalizer.normalize(query);

        final String[] queryParamPairs = actual.split("&");

        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=1");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "a=2");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "a=3");

        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "b=4");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[4], "c=111");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[5], "d=441");
    }

    @Test
    public void shouldNormalizeSplitCollections() {
        final String query = "a=1&b=4&c=111&a=3&d=441&a=2";
        final String actual = queryStringNormalizer.normalize(query);

        final String[] queryParamPairs = actual.split("&");

        // Notice that the values are not in order - we must preserve this
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=1");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "a=3");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "a=2");

        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "b=4");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[4], "c=111");
        assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[5], "d=441");
    }
}

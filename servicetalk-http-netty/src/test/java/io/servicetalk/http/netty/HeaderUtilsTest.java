/*
 * Copyright © 2019 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.http.netty;

import io.servicetalk.http.api.DefaultHttpHeadersFactory;
import io.servicetalk.http.api.HttpHeaders;
import io.servicetalk.http.api.HttpHeadersFactory;
import io.servicetalk.http.api.HttpRequest;

import org.junit.Before;
import org.junit.Test;

import static io.servicetalk.buffer.api.ReadOnlyBufferAllocators.DEFAULT_RO_ALLOCATOR;
import static io.servicetalk.http.api.HttpHeaderNames.CONTENT_LENGTH;
import static io.servicetalk.http.api.HttpHeaderNames.TRANSFER_ENCODING;
import static io.servicetalk.http.api.HttpHeaderValues.CHUNKED;
import static io.servicetalk.http.api.HttpProtocolVersions.HTTP_1_1;
import static io.servicetalk.http.api.HttpRequestMethods.GET;
import static io.servicetalk.http.api.HttpRequests.newRequest;
import static io.servicetalk.http.netty.HeaderUtils.addRequestTransferEncodingIfNecessary;
import static io.servicetalk.http.netty.HeaderUtils.isTransferEncodingChunked;
import static io.servicetalk.http.netty.HeaderUtils.setTransferEncodingChunked;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HeaderUtilsTest {

    private static final HttpHeadersFactory HTTP_HEADERS_FACTORY = DefaultHttpHeadersFactory.INSTANCE;

    private HttpRequest httpRequest;

    @Before
    public void setUp() {
        httpRequest = newRequest(GET, "/", HTTP_1_1,
                HTTP_HEADERS_FACTORY.newHeaders(), HTTP_HEADERS_FACTORY.newTrailers(), DEFAULT_RO_ALLOCATOR);
    }

    @Test
    public void isTransferEncodingChunkedFalseCases() {
        assertTrue(httpRequest.headers().isEmpty());
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));

        httpRequest.headers().add("Some-Header", "Some-Value");
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));

        httpRequest.headers().add(TRANSFER_ENCODING, "Some-Value");
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));
    }

    @Test
    public void isTransferEncodingChunkedTrueCases() {
        assertTrue(httpRequest.headers().isEmpty());
        // lower case
        httpRequest.headers().set(TRANSFER_ENCODING, CHUNKED);
        assertOneTransferEncodingChunked(httpRequest.headers());
        // Capital Case
        httpRequest.headers().set("Transfer-Encoding", "Chunked");
        assertOneTransferEncodingChunked(httpRequest.headers());
        // Random case
        httpRequest.headers().set(TRANSFER_ENCODING, "cHuNkEd");
        assertOneTransferEncodingChunked(httpRequest.headers());
    }

    @Test
    public void setTransferEncodingChunkedNoHeadersTrue() {
        assertTrue(httpRequest.headers().isEmpty());

        setTransferEncodingChunked(httpRequest.headers(), true);
        assertOneTransferEncodingChunked(httpRequest.headers());
        assertNull(httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void setTransferEncodingChunkedNoHeadersFalse() {
        assertTrue(httpRequest.headers().isEmpty());

        setTransferEncodingChunked(httpRequest.headers(), false);
        assertTrue(httpRequest.headers().isEmpty());
    }

    @Test
    public void setTransferEncodingChunkedWithSomeHeaderTrue() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add("Some-Header", "Some-Value");
        assertEquals(1, httpRequest.headers().size());

        setTransferEncodingChunked(httpRequest.headers(), true);
        assertTrue(isTransferEncodingChunked(httpRequest.headers()));
        assertEquals(2, httpRequest.headers().size());
        assertNull(httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void setTransferEncodingChunkedWithSomeHeaderFalse() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add("Some-Header", "Some-Value")
                .add(TRANSFER_ENCODING, "Some-Value")
                .add(TRANSFER_ENCODING, CHUNKED)
                .add("Transfer-Encoding", "Chunked")
                .add(TRANSFER_ENCODING, "cHuNkEd");
        assertEquals(5, httpRequest.headers().size());

        setTransferEncodingChunked(httpRequest.headers(), false);
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));
        assertEquals(2, httpRequest.headers().size());
        assertNull(httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void setTransferEncodingChunkedWithContentLengthTrue() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(CONTENT_LENGTH, "10");
        assertEquals(1, httpRequest.headers().size());

        setTransferEncodingChunked(httpRequest.headers(), true);
        assertOneTransferEncodingChunked(httpRequest.headers());
        assertNull(httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void setTransferEncodingChunkedWithContentLengthFalse() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(CONTENT_LENGTH, "10")
                .add(TRANSFER_ENCODING, CHUNKED);
        assertEquals(2, httpRequest.headers().size());

        setTransferEncodingChunked(httpRequest.headers(), false);
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));
        assertEquals(1, httpRequest.headers().size());
        assertEquals("10", httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void setTransferEncodingChunkedWithMultipleContentLength() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(CONTENT_LENGTH, "10")
                .add(CONTENT_LENGTH, "20")
                .add(CONTENT_LENGTH, "30");
        assertEquals(3, httpRequest.headers().size());

        setTransferEncodingChunked(httpRequest.headers(), true);
        assertOneTransferEncodingChunked(httpRequest.headers());
        assertNull(httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void addRequestTransferEncodingIfNecessaryNoHeaders() {
        assertTrue(httpRequest.headers().isEmpty());
        addRequestTransferEncodingIfNecessary(httpRequest);
        assertOneTransferEncodingChunked(httpRequest.headers());
    }

    @Test
    public void addRequestTransferEncodingIfNecessaryContentLength() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(CONTENT_LENGTH, "10");
        assertEquals(1, httpRequest.headers().size());

        addRequestTransferEncodingIfNecessary(httpRequest);

        assertEquals(1, httpRequest.headers().size());
        assertFalse(isTransferEncodingChunked(httpRequest.headers()));
        assertEquals("10", httpRequest.headers().get(CONTENT_LENGTH));
    }

    @Test
    public void addRequestTransferEncodingIfNecessaryTransferEncodingChunked() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(TRANSFER_ENCODING, CHUNKED);
        assertEquals(1, httpRequest.headers().size());

        addRequestTransferEncodingIfNecessary(httpRequest);
        assertOneTransferEncodingChunked(httpRequest.headers());
    }

    @Test
    public void addRequestTransferEncodingIfNecessaryTransferEncodingChunkedCapitalCase() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add("Transfer-Encoding", "Chunked");
        assertEquals(1, httpRequest.headers().size());

        addRequestTransferEncodingIfNecessary(httpRequest);
        assertOneTransferEncodingChunked(httpRequest.headers());
    }

    @Test
    public void addRequestTransferEncodingIfNecessaryTransferEncodingChunkedRandomCase() {
        assertTrue(httpRequest.headers().isEmpty());
        httpRequest.headers().add(TRANSFER_ENCODING, "cHuNkEd");
        assertEquals(1, httpRequest.headers().size());

        addRequestTransferEncodingIfNecessary(httpRequest);
        assertOneTransferEncodingChunked(httpRequest.headers());
    }

    private static void assertOneTransferEncodingChunked(final HttpHeaders headers) {
        assertEquals(1, headers.size());
        assertTrue(isTransferEncodingChunked(headers));
    }
}
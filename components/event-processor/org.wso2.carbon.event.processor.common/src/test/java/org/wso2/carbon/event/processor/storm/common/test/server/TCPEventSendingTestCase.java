/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.event.processor.storm.common.test.server;

import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.event.processor.common.transport.client.TCPEventPublisher;
import org.wso2.carbon.event.processor.common.transport.server.TCPEventServer;
import org.wso2.carbon.event.processor.common.transport.server.TCPEventServerConfig;
import org.wso2.carbon.event.processor.common.transport.server.StreamCallback;
import org.wso2.carbon.event.processor.storm.common.test.util.AnalyticStatDataProvider;
import org.wso2.carbon.event.processor.storm.common.test.util.DataProvider;
import org.wso2.carbon.event.processor.storm.common.test.util.SimpleDataProvider;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPEventSendingTestCase {
    public static final int EVENTS_PER_CLIENT = 10;
    public static final int TOTAL_CLIENTS = 5;
    private static final Log log = LogFactory.getLog(TCPEventSendingTestCase.class);
    private ExecutorService threadPool;

    @Before
    public void initialize() {
        threadPool = Executors.newFixedThreadPool(20);
    }

    @Test
    public void testEventSendingToServer() {

        StreamDefinition streamDefinition = new StreamDefinition().id("TestStream")
                .attribute("att1", Attribute.Type.INT)
                .attribute("att2", Attribute.Type.FLOAT)
                .attribute("att3", Attribute.Type.STRING)
                .attribute("att4", Attribute.Type.INT);

        TestStreamCallback streamCallback = new TestStreamCallback();
        TCPEventServer TCPEventServer = new TCPEventServer(new TCPEventServerConfig(7612), streamCallback);
        try {
            TCPEventServer.subscribe(streamDefinition);
            TCPEventServer.start();
            Thread.sleep(1000);
            threadPool.submit(new ClientThread(streamDefinition, new SimpleDataProvider(), 100));
            Thread.sleep(5000);
            Assert.assertEquals(100, streamCallback.getEventCount());
            log.info("Shutting down server...");
            TCPEventServer.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighLoadEventSendingToServer() {

        StreamDefinition streamDefinition = new StreamDefinition().id("analyticsStats")
                .attribute("meta_ipAdd", Attribute.Type.STRING)
                .attribute("meta_index", Attribute.Type.LONG)
                .attribute("meta_timestamp", Attribute.Type.LONG)
                .attribute("meta_nanoTime", Attribute.Type.LONG)
                .attribute("userID", Attribute.Type.STRING)
                .attribute("searchTerms", Attribute.Type.STRING);

        TestStreamCallback streamCallback = new TestStreamCallback();
        TCPEventServer TCPEventServer = new TCPEventServer(new TCPEventServerConfig(7612), streamCallback);
        try {
            TCPEventServer.subscribe(streamDefinition);
            TCPEventServer.start();
            Thread.sleep(1000);
            for (int i = 0; i < TOTAL_CLIENTS; i++) {
                threadPool.submit(new ClientThread(streamDefinition, new AnalyticStatDataProvider(), EVENTS_PER_CLIENT));
            }
            while (streamCallback.getEventCount() < TOTAL_CLIENTS * EVENTS_PER_CLIENT) {
                Thread.sleep(5000);
            }
            Assert.assertEquals(TOTAL_CLIENTS * EVENTS_PER_CLIENT, streamCallback.getEventCount());
            log.info("Shutting down server...");
            TCPEventServer.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class TestStreamCallback implements StreamCallback {
        AtomicInteger eventCount = new AtomicInteger(0);

        @Override
        public void receive(String streamId, Object[] event) {
            log.info("Event count:" + eventCount.incrementAndGet() + ", Stream ID: " + streamId + ", Event: " + Arrays.deepToString(event));
        }

        public int getEventCount() {
            return eventCount.get();
        }
    }

    private static class ClientThread implements Runnable {
        int eventsToSend = 0;
        StreamDefinition streamDefinition;
        DataProvider dataProvider;

        public ClientThread(StreamDefinition streamDefinition, DataProvider dataProvider, int eventsToSend) {
            this.eventsToSend = eventsToSend;
            this.streamDefinition = streamDefinition;
            this.dataProvider = dataProvider;
        }

        @Override
        public void run() {
            TCPEventPublisher TCPEventPublisher = null;
            try {
                TCPEventPublisher = new TCPEventPublisher("localhost:7612",true);
                TCPEventPublisher.addStreamDefinition(streamDefinition);
                Thread.sleep(1000);
                log.info("Starting event client to send events to localhost:7612");

                for (int i = 0; i < eventsToSend; i++) {
                    TCPEventPublisher.sendEvent(streamDefinition.getId(), dataProvider.getEvent(),true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (TCPEventPublisher != null) {
                    TCPEventPublisher.shutdown();
                }
            }
        }


    }
}

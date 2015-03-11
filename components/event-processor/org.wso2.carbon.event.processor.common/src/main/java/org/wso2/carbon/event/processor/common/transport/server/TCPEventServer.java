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

package org.wso2.carbon.event.processor.common.transport.server;

import org.apache.log4j.Logger;
import org.wso2.carbon.event.processor.common.transport.common.EventServerUtils;
import org.wso2.carbon.event.processor.common.transport.common.StreamRuntimeInfo;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEventServer {
    private static Logger log = Logger.getLogger(TCPEventServer.class);
    private TCPEventServerConfig TCPEventServerConfig = new TCPEventServerConfig(7211);
    private ExecutorService pool;
    private StreamCallback streamCallback;
    private ServerWorker serverWorker;
    private Map<String, StreamRuntimeInfo> streamRuntimeInfoMap = new ConcurrentHashMap<String, StreamRuntimeInfo>();

    public TCPEventServer(TCPEventServerConfig TCPEventServerConfig, StreamCallback streamCallback) {
        this.TCPEventServerConfig = TCPEventServerConfig;
        this.streamCallback = streamCallback;
        this.serverWorker = new ServerWorker();
        this.pool = Executors.newFixedThreadPool(TCPEventServerConfig.getNumberOfThreads());
    }

    public void subscribe(StreamDefinition streamDefinition) {
        String streamId = streamDefinition.getId();
        this.streamRuntimeInfoMap.put(streamId, EventServerUtils.createStreamRuntimeInfo(streamDefinition));
    }

    public void start() {
        if (!serverWorker.isRunning()) {
            new Thread(serverWorker).start();
        }
    }

    public void shutdown() {
        serverWorker.shutdownServerWorker();
    }

    private class ServerWorker implements Runnable {
        private ServerSocket receiverSocket;
        private boolean isRunning = false;

        public boolean isRunning() {
            return isRunning;
        }

        public void shutdownServerWorker() {
            isRunning = false;
            try {
                receiverSocket.close();
            } catch (IOException e) {
                log.error("Error occurred while trying to shutdown socket: " + e.getMessage(), e);
            }
        }

        @Override
        public void run() {
            try {
                log.info("EventServer starting event listener on port " + TCPEventServerConfig.getPort());
                isRunning = true;
                receiverSocket = new ServerSocket(TCPEventServerConfig.getPort());
                while (isRunning) {
                    final Socket connectionSocket = receiverSocket.accept();
                    pool.execute(new ListenerProcessor(connectionSocket));
                }
            } catch (Throwable e) {
                if (isRunning) {
                    log.error("Error while the server was listening for events: " + e.getMessage(), e);
                } else {
                    log.info("EventServer stopped listening for socket connections.");
                }
            } finally {
                isRunning = false;
            }
        }

        private class ListenerProcessor implements Runnable {

            private final Socket connectionSocket;

            public ListenerProcessor(Socket connectionSocket) {
                this.connectionSocket = connectionSocket;
            }

            @Override
            public void run() {
                try {
                    BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream());
                    while (true) {

                        int streamNameSize = loadData(in) & 0xff;
                        byte[] streamNameData = loadData(in, new byte[streamNameSize]);
                        String streamId = new String(streamNameData, 0, streamNameData.length);
                        StreamRuntimeInfo streamRuntimeInfo = streamRuntimeInfoMap.get(streamId);

                        Object[] event = new Object[streamRuntimeInfo.getNoOfAttributes()];
                        byte[] fixedMessageData = loadData(in, new byte[streamRuntimeInfo.getFixedMessageSize()]);

                        ByteBuffer bbuf = ByteBuffer.wrap(fixedMessageData, 0, fixedMessageData.length);
                        Attribute.Type[] attributeTypes = streamRuntimeInfo.getAttributeTypes();
                        for (int i = 0; i < attributeTypes.length; i++) {
                            Attribute.Type type = attributeTypes[i];
                            switch (type) {
                                case INT:
                                    event[i] = bbuf.getInt();
                                    continue;
                                case LONG:
                                    event[i] = bbuf.getLong();
                                    continue;
                                case BOOL:
                                    event[i] = bbuf.get() == 1;
                                    continue;
                                case FLOAT:
                                    event[i] = bbuf.getFloat();
                                    continue;
                                case DOUBLE:
                                    event[i] = bbuf.getLong();
                                    continue;
                                case STRING:
                                    int size = bbuf.getShort() & 0xffff;
                                    byte[] stringData = loadData(in, new byte[size]);
                                    event[i] = new String(stringData, 0, stringData.length);
                            }
                        }
                        streamCallback.receive(streamId, event);
                    }
                } catch (EOFException e) {
                    log.info("Closing listener socket. " + e.getMessage());
                } catch (IOException e) {
                    log.error("Error reading data from receiver socket:" + e.getMessage(), e);
                } catch (Throwable t){
                    log.error("Error :"  + t.getMessage(), t);
                }
            }

            /**
             * Returns the number of bytes that were read by the stream.
             * This method does not return if the stream is closed from remote end.
             *
             * @param in the input stream to be read
             * @return the number of bytes read
             * @throws IOException
             */
            private int loadData(BufferedInputStream in) throws IOException {
                int byteData = in.read();
                if (byteData != -1) {
                    return byteData;
                } else {
                    throw new EOFException("Connection closed from remote end.");
                }
            }

            private byte[] loadData(BufferedInputStream in, byte[] dataArray) throws IOException {

                int start = 0;
                while (true) {
                    int readCount = in.read(dataArray, start, dataArray.length - start);
                    if (readCount != -1) {
                        start += readCount;
                        if (start == dataArray.length) {
                            return dataArray;
                        }
                    } else {
                        throw new EOFException("Connection closed from remote end.");
                    }
                }
            }

        }
    }
}

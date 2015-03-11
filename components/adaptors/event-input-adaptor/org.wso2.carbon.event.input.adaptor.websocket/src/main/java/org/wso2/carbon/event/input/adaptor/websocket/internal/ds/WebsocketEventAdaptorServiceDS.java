/*
<<<<<<< HEAD
 * Copyright 2004,2005 The Apache Software Foundation.
 *
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
 */

=======
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
>>>>>>> f2861ad70422867e7d5031a219a3f5415c6553f9
package org.wso2.carbon.event.input.adaptor.websocket.internal.ds;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.event.input.adaptor.core.InputEventAdaptorFactory;
import org.wso2.carbon.event.input.adaptor.websocket.WebsocketEventAdaptorFactory;

<<<<<<< HEAD

/**
 * @scr.component name="input.websocketInputService.component" immediate="true"
 */


=======
/**
 * @scr.component name="input.websocketInputService.component" immediate="true"
 */
>>>>>>> f2861ad70422867e7d5031a219a3f5415c6553f9
public class WebsocketEventAdaptorServiceDS {

    private static final Log log = LogFactory.getLog(WebsocketEventAdaptorServiceDS.class);

    /**
     * initialize the agent service here service here.
     *
     * @param context
     */
<<<<<<< HEAD


=======
>>>>>>> f2861ad70422867e7d5031a219a3f5415c6553f9
    protected void activate(ComponentContext context) {

        try {
            InputEventAdaptorFactory websocketEventAdaptorFactory = new WebsocketEventAdaptorFactory();
            context.getBundleContext().registerService(InputEventAdaptorFactory.class.getName(), websocketEventAdaptorFactory, null);
            if (log.isDebugEnabled()) {
                log.debug("Successfully deployed the input websocket adaptor service");
            }
<<<<<<< HEAD
            log.info("Successfully deployed the input websocket adaptor service");

=======
>>>>>>> f2861ad70422867e7d5031a219a3f5415c6553f9
        } catch (RuntimeException e) {
            log.error("Can not create the input websocket adaptor service ", e);
        }
    }
<<<<<<< HEAD

=======
>>>>>>> f2861ad70422867e7d5031a219a3f5415c6553f9
}

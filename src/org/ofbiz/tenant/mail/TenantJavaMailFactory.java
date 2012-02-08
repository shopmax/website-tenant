/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.tenant.mail;

import java.util.Map;
import java.util.Timer;

import javax.mail.Session;
import javax.mail.Store;

import org.ofbiz.base.util.UtilValidate;

import javolution.util.FastMap;

public class TenantJavaMailFactory {

    public final static String module = TenantJavaMailFactory.class.getName();
    
    protected static Map<String, Timer> pollTimers = FastMap.newInstance();
    protected static Map<String, Session> sessions = FastMap.newInstance();
    protected static Map<String, Store> stores = FastMap.newInstance();
    
    /**
     * get poll timer
     * @param tenantId
     * @return
     */
    public static Timer getPollTimer(String tenantId) {
        Timer pollTimer = pollTimers.get(tenantId);
        if (UtilValidate.isEmpty(pollTimer)) {
            pollTimer = new Timer();
        }
        return pollTimer;
    }
    
    /**
     * set session
     * @param tenantId
     * @param session
     */
    public static void setSession(String tenantId, Session session) {
        sessions.put(tenantId, session);
    }
    
    /**
     * get session
     * @param tenantId
     * @return
     */
    public static Session getSession(String tenantId) {
        return sessions.get(tenantId);
    }
    
    /**
     * set store
     * @param tenantId
     * @param store
     */
    public static void setStore(String tenantId, Store store) {
        stores.put(tenantId, store);
    }
    
    /**
     * get store
     * @param tenantId
     * @return
     */
    public static Store getStore(String tenantId) {
        return stores.get(tenantId);
    }
}

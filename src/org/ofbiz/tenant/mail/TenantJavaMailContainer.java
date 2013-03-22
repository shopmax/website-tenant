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

import java.util.List;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceContainer;

/**
 * Tenant JavaMail Container
 * @author chatree
 *
 */
public class TenantJavaMailContainer implements Container {
    
    public final static String module = TenantJavaMailContainer.class.getName();
    
    protected String name = null;
    protected String configFile = null;

    @Override
    public void init(String[] args, String name, String configFile)
            throws ContainerException {
        this.name = name;
        this.configFile = configFile;
    }

    @Override
    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer("tenant-javamail-container", configFile);
        String delegatorName = ContainerConfig.getPropertyValue(cfg, "delegator-name", "default");
        Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
        
        try {
            String multitenant = EntityUtilProperties.getPropertyValue("general", "multitenant", delegator);
            if (UtilValidate.isNotEmpty(multitenant) && multitenant.equalsIgnoreCase("Y")) {
                GenericValue systemUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
                
                // get all tenants
                List<GenericValue> tenants = delegator.findList("Tenant", null, null, null, null, false);
                for (GenericValue tenant : tenants) {
                    String tenantId = tenant.getString("tenantId");
                    /*
                    boolean isConnectionAvailable = TenantUtil.isConnectionAvailable(tenantId, delegator);
                    
                    if (isConnectionAvailable) {
                        try {
                            String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
                            Delegator tenantDelegator = DelegatorFactory.getDelegator(tenantDelegatorName);
                            
                            // get mail properties
                            String mailStoreProtocol = EntityUtilProperties.getPropertyValue("mail", "mail.store.protocol", tenantDelegator);
                            String mailHost = EntityUtilProperties.getPropertyValue("mail", "mail.host", tenantDelegator);
                            String mailUser = EntityUtilProperties.getPropertyValue("mail", "mail.user", tenantDelegator);
                            String mailPass = EntityUtilProperties.getPropertyValue("mail", "mail.pass", tenantDelegator);
                            
                            // if mail properties are completed then schedule a mail listener
                            if (UtilValidate.isNotEmpty(mailStoreProtocol) && UtilValidate.isNotEmpty(mailHost)
                                    && UtilValidate.isNotEmpty(mailUser) && UtilValidate.isNotEmpty(mailPass)) {
                                
                                // create a JavaMail listener
                                try {
                                    Map<String, Object> createTenantJavaMailListenerInMap = FastMap.newInstance();
                                    createTenantJavaMailListenerInMap.put("tenantId", tenantId);
                                    createTenantJavaMailListenerInMap.put("mailStoreProtocol", mailStoreProtocol);
                                    createTenantJavaMailListenerInMap.put("mailHost", mailHost);
                                    createTenantJavaMailListenerInMap.put("mailUser", mailUser);
                                    createTenantJavaMailListenerInMap.put("mailPass", mailPass);
                                    createTenantJavaMailListenerInMap.put("userLogin", systemUserLogin);
                                    dispatcher.runSync("createTenantJavaMailListener", createTenantJavaMailListenerInMap);
                                    
                                    // schedule java mail poller task
                                    Map<String, Object> scheduleTenantJavaMailPollerTaskInMap = FastMap.newInstance();
                                    scheduleTenantJavaMailPollerTaskInMap.put("tenantId", tenantId);
                                    scheduleTenantJavaMailPollerTaskInMap.put("maxSize", 1000000L);
                                    scheduleTenantJavaMailPollerTaskInMap.put("timerDelay", 300000L);
                                    scheduleTenantJavaMailPollerTaskInMap.put("userLogin", systemUserLogin);
                                    Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
                                    runTenantServiceInMap.put("tenantId", tenantId);
                                    runTenantServiceInMap.put("serviceName", "scheduleTenantJavaMailPollerTask");
                                    runTenantServiceInMap.put("serviceParameters", scheduleTenantJavaMailPollerTaskInMap);
                                    runTenantServiceInMap.put("isAsync", Boolean.FALSE);
                                    runTenantServiceInMap.put("userLogin", systemUserLogin);
                                    dispatcher.runSync("runTenantService", runTenantServiceInMap);
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, module);
                                }
                            }
                        } catch (Exception e) {
                            Debug.logWarning("Could not start JavaMail for tenant: " + tenantId, module);
                            continue;
                        }
                    }
                    */
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return false;
    }

    @Override
    public void stop() throws ContainerException {
        
    }

    @Override
    public String getName() {
        return name;
    }
}

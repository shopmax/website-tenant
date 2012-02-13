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
package org.ofbiz.tenant.tenant;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Tenant worker
 * @author chatree
 *
 */
public class TenantWorker {

    public final static String module = TenantWorker.class.getName();
    
    /**
     * set service fields
     * @param serviceName
     * @param context
     * @param toContext
     * @param timeZone
     * @param locale
     * @param dispatcher
     * @return
     */
    public static Map<String, Object> setServiceFields(String serviceName, Map<String, Object> context, Map<String, Object> toContext, TimeZone timeZone, Locale locale, LocalDispatcher dispatcher) {
        ModelService modelService = null;
        List<Object> errorMessages = FastList.newInstance();
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "In set-service-fields could not get service definition for service name [" + serviceName + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        toContext.putAll(modelService.makeValid(context, "IN", true, errorMessages, timeZone, locale));
        return ServiceUtil.returnSuccess();
    }
}

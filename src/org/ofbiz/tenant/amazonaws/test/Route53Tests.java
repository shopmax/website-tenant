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
package org.ofbiz.tenant.amazonaws.test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.testtools.OFBizTestCase;

import com.amazonaws.services.route53.model.DelegationSet;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ResourceRecordSet;

/**
 * Route53 Tests
 * @author chatree
 *
 */
public class Route53Tests extends OFBizTestCase {
    
    public final static String module = Route53Tests.class.getName();

    protected GenericValue userLogin = null;

    public Route53Tests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
    }

    public void testGetAmazonRoute53HostedZones() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("getAmazonRoute53HostedZones", ctx);
        List<HostedZone> hostedZones = UtilGenerics.checkList(results.get("hostedZones"));
        for (HostedZone hostedZone : hostedZones) {
            Debug.logInfo("-- hostedZone: " + hostedZone, module);
        }
    }

    public void testGetAmazonRoute53HostedZone() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("hostedZoneId", "ZSL0SZERL2LEB");
        ctx.put("userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("getAmazonRoute53HostedZone", ctx);
        HostedZone hostedZone = (HostedZone) results.get("hostedZone");
        DelegationSet delegationSet = (DelegationSet) results.get("delegationSet");
        Debug.logInfo("-- hostedZone: " + hostedZone, module);
        Debug.logInfo("-- delegationSet: " + delegationSet, module);
    }

    public void testGetAmazonRoute53ResourceRecordSets() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("hostedZoneId", "ZSL0SZERL2LEB");
        ctx.put("userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("getAmazonRoute53ResourceRecordSets", ctx);
        List<ResourceRecordSet> resourceRecordSets = UtilGenerics.checkList(results.get("resourceRecordSets"));
        for (ResourceRecordSet resourceRecordSet : resourceRecordSets) {
            Debug.logInfo("-- resourceRecordSet: " + resourceRecordSet, module);
        }
    }

    public void testCreateAmazonRoute53ResourceRecordSet() throws Exception {
        List<String> domainNames = UtilMisc.toList("\"test\"");
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("hostedZoneId", "ZSL0SZERL2LEB");
        ctx.put("recordSetName", "ofbizsaas.com");
        ctx.put("recordSetType", "TXT");
        ctx.put("resourceRecordSetId", "testtest");
        ctx.put("domainNames", domainNames);
        //ctx.put("weight", Long.valueOf("0"));
        //ctx.put("tTL", Long.valueOf("300"));
        ctx.put("userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("createAmazonRoute53ResourceRecordSet", ctx);
        if (ServiceUtil.isSuccess(results)) {
            String status = (String) results.get("status");
            Date submittedAt = (Date) results.get("submittedAt");
            String comments = (String) results.get("comments");
            Debug.logInfo("-- status: " + status, module);
            Debug.logInfo("-- submittedAt: " + submittedAt, module);
            Debug.logInfo("-- comments: " + comments, module);
        } else {
            String errMsg = (String) results.get("errorMessage");
            Debug.logError(errMsg, module);
        }
    }

    public void testDeleteAmazonRoute53ResourceRecordSet() throws Exception {
        List<String> domainNames = UtilMisc.toList("\"test\"");
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("hostedZoneId", "ZSL0SZERL2LEB");
        ctx.put("recordSetName", "ofbizsaas.com");
        ctx.put("recordSetType", "TXT");
        ctx.put("resourceRecordSetId", "testtest");
        ctx.put("domainNames", domainNames);
        //ctx.put("weight", Long.valueOf("0"));
        //ctx.put("tTL", Long.valueOf("300"));
        ctx.put("userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("deleteAmazonRoute53ResourceRecordSet", ctx);
        if (ServiceUtil.isSuccess(results)) {
            String status = (String) results.get("status");
            Date submittedAt = (Date) results.get("submittedAt");
            String comments = (String) results.get("comments");
            Debug.logInfo("-- status: " + status, module);
            Debug.logInfo("-- submittedAt: " + submittedAt, module);
            Debug.logInfo("-- comments: " + comments, module);
        } else {
            String errMsg = (String) results.get("errorMessage");
            Debug.logError(errMsg, module);
        }
    }
}

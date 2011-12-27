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
package org.ofbiz.tenant.amazonaws;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.tenant.amazonaws.util.AwsUtil;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.DelegationSet;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.GetHostedZoneResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

public class AwsServices {

    public final static String module = AwsServices.class.getName();
    
    /**
     * get Amazon Rout53 hosted zones
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> getAmazonRoute53HostedZones(DispatchContext ctx, Map<String, Object> context) {
        AmazonRoute53 route53 = AwsFactory.getAmazonRoute53();
        ListHostedZonesResult hostedZonesResult = route53.listHostedZones();
        List<HostedZone> hostedZones = hostedZonesResult.getHostedZones();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("hostedZones", hostedZones);
        return results;
    }
    
    /**
     * get Amazon Rout53 hosted zone
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> getAmazonRoute53HostedZone(DispatchContext ctx, Map<String, Object> context) {
        String hostedZoneId = (String) context.get("hostedZoneId");
        
        AmazonRoute53 route53 = AwsFactory.getAmazonRoute53();
        GetHostedZoneRequest request = new GetHostedZoneRequest(hostedZoneId);
        GetHostedZoneResult hostedZoneResult = route53.getHostedZone(request);
        HostedZone hostedZone = hostedZoneResult.getHostedZone();
        if (UtilValidate.isEmpty(hostedZone)) {
            return ServiceUtil.returnError("Could not find hosted zone: " + hostedZoneId);
        }
        DelegationSet delegationSet = hostedZoneResult.getDelegationSet();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("hostedZone", hostedZone);
        results.put("delegationSet", delegationSet);
        return results;
    }
    
    /**
     * get Amazon Rout53 resource record sets
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> getAmazonRoute53ResourceRecordSets(DispatchContext ctx, Map<String, Object> context) {
        String hostedZoneId = (String) context.get("hostedZoneId");
        
        AmazonRoute53 route53 = AwsFactory.getAmazonRoute53();
        ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest(hostedZoneId);
        ListResourceRecordSetsResult resourceRecordsetsResult = route53.listResourceRecordSets(request);
        List<ResourceRecordSet> resourceRecordSets = resourceRecordsetsResult.getResourceRecordSets();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("resourceRecordSets", resourceRecordSets);
        return results;
    }
    
    /**
     * create Amazon Rout53 resource record set
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> createAmazonRoute53ResourceRecordSet(DispatchContext ctx, Map<String, Object> context) {
        String hostedZoneId = (String) context.get("hostedZoneId");
        String recordSetName = (String) context.get("recordSetName");
        String recordSetType = (String) context.get("recordSetType");
        List<String> domainNames = UtilGenerics.checkList(context.get("domainNames"));
        String dNSName = (String) context.get("dNSName");
        String resourceRecordSetId = (String) context.get("resourceRecordSetId");
        Long tTL = (Long) context.get("tTL");
        
        try {
            AmazonRoute53 route53 = AwsFactory.getAmazonRoute53();
            RRType rrType = AwsUtil.getRRType(recordSetType);
            ResourceRecordSet resourceRecordSet = new ResourceRecordSet(recordSetName, rrType);
            
            // set alias target
            if (UtilValidate.isNotEmpty(dNSName)) {
                AliasTarget aliasTarget = new AliasTarget(hostedZoneId, dNSName);
                resourceRecordSet.setAliasTarget(aliasTarget);
            }
            
            // set resource record set identifier
            if (UtilValidate.isNotEmpty(resourceRecordSetId)) {
                resourceRecordSet.setSetIdentifier(resourceRecordSetId);
            }

            // set resource records
            List<ResourceRecord> resourceRecords = FastList.newInstance();
            for (String domainName : domainNames) {
                ResourceRecord resourceRecord = new ResourceRecord(domainName);
                resourceRecords.add(resourceRecord);
            }
            resourceRecordSet.setResourceRecords(resourceRecords);
            
            resourceRecordSet.setTTL(tTL);
            Change change = new Change(ChangeAction.CREATE, resourceRecordSet);
            List<Change> changes = FastList.newInstance();
            changes.add(change);
            ChangeBatch changeBatch = new ChangeBatch(changes);
            ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest(hostedZoneId, changeBatch);
            ChangeResourceRecordSetsResult resourceRecordSetsResult = route53.changeResourceRecordSets(request);
            ChangeInfo changeInfo = resourceRecordSetsResult.getChangeInfo();
            String status = changeInfo.getStatus();
            Date submittedAt = changeInfo.getSubmittedAt();
            String comment = changeInfo.getComment();
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("status", status);
            results.put("submittedAt", submittedAt);
            results.put("comments", comment);
            return results;
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * delete Amazon Rout53 resource record sets
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteAmazonRoute53ResourceRecordSet(DispatchContext ctx, Map<String, Object> context) {
        String hostedZoneId = (String) context.get("hostedZoneId");
        String recordSetName = (String) context.get("recordSetName");
        String recordSetType = (String) context.get("recordSetType");
        String dNSName = (String) context.get("dNSName");
        String resourceRecordSetId = (String) context.get("resourceRecordSetId");
        
        try {
            AmazonRoute53 route53 = AwsFactory.getAmazonRoute53();
            RRType rrType = AwsUtil.getRRType(recordSetType);
            ResourceRecordSet resourceRecordSet = new ResourceRecordSet(recordSetName, rrType);
            
            // set alias target
            if (UtilValidate.isNotEmpty(dNSName)) {
                AliasTarget aliasTarget = new AliasTarget(hostedZoneId, dNSName);
                resourceRecordSet.setAliasTarget(aliasTarget);
            }
            
            // set resource record set identifier
            if (UtilValidate.isNotEmpty(resourceRecordSetId)) {
                resourceRecordSet.setSetIdentifier(resourceRecordSetId);
            }
            
            Change change = new Change(ChangeAction.DELETE, resourceRecordSet);
            List<Change> changes = FastList.newInstance();
            changes.add(change);
            ChangeBatch changeBatch = new ChangeBatch(changes);
            ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest(hostedZoneId, changeBatch);
            ChangeResourceRecordSetsResult resourceRecordSetsResult = route53.changeResourceRecordSets(request);
            ChangeInfo changeInfo = resourceRecordSetsResult.getChangeInfo();
            String status = changeInfo.getStatus();
            Date submittedAt = changeInfo.getSubmittedAt();
            String comment = changeInfo.getComment();
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("status", status);
            results.put("submittedAt", submittedAt);
            results.put("comments", comment);
            return results;
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.io.FileUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.entityext.data.EntityDataLoadContainer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceContainer;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.tenant.jdbc.TenantConnectionFactory;
import org.ofbiz.tenant.jdbc.TenantJdbcConnectionHandler;
import org.ofbiz.tenant.util.TenantUtil;

/**
 * Tenant Services
 * @author chatree
 *
 */
public class TenantServices {

    public final static String module = TenantServices.class.getName();
    
    /**
     * install tenants
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> installTenants(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        
        try {
            List<EntityCondition> conds = FastList.newInstance();
            conds.add(EntityCondition.makeCondition(EntityJoinOperator.OR
                    , EntityCondition.makeCondition("disabled", null)
                    , EntityCondition.makeCondition(EntityFunction.UPPER("disabled"), EntityComparisonOperator.NOT_EQUAL, "N")));
            List<GenericValue> tenants = delegator.findList("Tenant", null, null, null, null, false);
            for (GenericValue tenant : tenants) {
                String tenantId = tenant.getString("tenantId");
                Map<String, Object> installTenantDataSourcesInMap = FastMap.newInstance();
                installTenantDataSourcesInMap.put("tenantId", tenantId);
                Map<String, Object> results = dispatcher.runSync("installTenantDataSources", installTenantDataSourcesInMap);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError("Could not install tenant data sources");
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * Export tenant all entities
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> exportTenantAllEntities(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        String outpath = (String) context.get("outpath");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Integer txTimeout = (Integer) context.get("txTimeout");
        
        try {
            Map<String, Object> serviceParameters = FastMap.newInstance();
            serviceParameters.put("outpath", outpath);
            serviceParameters.put("fromDate", fromDate);
            serviceParameters.put("txTimeout", txTimeout);
            serviceParameters.put("userLogin", userLogin);
            Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
            runTenantServiceInMap.put("tenantId", tenantId);
            runTenantServiceInMap.put("serviceName", "entityExportAll");
            runTenantServiceInMap.put("serviceParameters", serviceParameters);
            runTenantServiceInMap.put("userLogin", userLogin);
            Map<String, Object> results = dispatcher.runSync("runTenantService", runTenantServiceInMap);
            Map<String, Object> serviceResults = UtilGenerics.cast(results.get("serviceResults"));
            return serviceResults;
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * Import tenant entity directory
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> importTenantEntityDir(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        String path = (String) context.get("path");
        String mostlyInserts = (String) context.get("mostlyInserts");
        String maintainTimeStamps = (String) context.get("maintainTimeStamps");
        String createDummyFks = (String) context.get("createDummyFks");
        String checkDataOnly = (String) context.get("checkDataOnly");
        String deleteFiles = (String) context.get("deleteFiles");
        Integer txTimeout = (Integer) context.get("txTimeout");
        Long filePause = (Long) context.get("filePause");
        
        try {
            Map<String, Object> serviceParameters = FastMap.newInstance();
            serviceParameters.put("path", path);
            serviceParameters.put("mostlyInserts", mostlyInserts);
            serviceParameters.put("maintainTimeStamps", maintainTimeStamps);
            serviceParameters.put("createDummyFks", createDummyFks);
            serviceParameters.put("checkDataOnly", checkDataOnly);
            serviceParameters.put("deleteFiles", deleteFiles);
            serviceParameters.put("txTimeout", txTimeout);
            serviceParameters.put("filePause", filePause);
            serviceParameters.put("userLogin", userLogin);
            Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
            runTenantServiceInMap.put("tenantId", tenantId);
            runTenantServiceInMap.put("serviceName", "entityImportDir");
            runTenantServiceInMap.put("serviceParameters", serviceParameters);
            runTenantServiceInMap.put("userLogin", userLogin);
            Map<String, Object> results = dispatcher.runSync("runTenantService", runTenantServiceInMap);
            Map<String, Object> serviceResults = UtilGenerics.cast(results.get("serviceResults"));
            return serviceResults;
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * Create tenant backup
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> createTenantBackup(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        
        try {
            Date nowDate = UtilDateTime.nowDate();
            String suffix = new SimpleDateFormat("yyyyMMddhhmmssSSS").format(nowDate);
            GenericValue sysUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
            String outpath = EntityUtilProperties.getPropertyValue("tenant", "backup.outpath", delegator);
            File tempDir = new File(outpath, "tmp_" + tenantId + "_" + suffix);
            tempDir.mkdir();
            Map<String, Object> exportTenantAllEntitiesInMap = FastMap.newInstance();
            exportTenantAllEntitiesInMap.put("tenantId", tenantId);
            exportTenantAllEntitiesInMap.put("outpath", tempDir.getAbsolutePath());
            exportTenantAllEntitiesInMap.put("userLogin", sysUserLogin);
            Map<String, Object> exportTenantAllEntitiesResults = dispatcher.runSync("exportTenantAllEntities", exportTenantAllEntitiesInMap);
            List<String> results = UtilGenerics.cast(exportTenantAllEntitiesResults.get("results"));
            String firstResult = results.get(0);
            if (firstResult.startsWith("[")) {
                // create zip file
                File outFile = new File(outpath + "/" + tenantId + "_" + suffix + ".zip");
                ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
                String[] fileList = tempDir.list();
                byte[] readBuffer = new byte[2156]; 
                int bytesIn = 0;
                for(int i = 0; i < fileList.length; i++) {
                    File file = new File(tempDir, fileList[i]);
                    FileInputStream fis = new FileInputStream(file);
                    ZipEntry entry = new ZipEntry(file.getName());
                    zout.putNextEntry(entry);
                    while((bytesIn = fis.read(readBuffer)) != -1) { 
                        zout.write(readBuffer, 0, bytesIn); 
                    }
                    fis.close();
                }
                zout.close();
                FileUtils.deleteDirectory(tempDir);
                
                // create party content
                String contentName = null;
                GenericValue tenantUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
                if (UtilValidate.isNotEmpty(tenantUserLogin)) {
                    String partyId = tenantUserLogin.getString("partyId");
                    contentName = "Data of " + tenantId + " at " + nowDate.toString();
                    String dataResourceName = contentName.replace(" ", "_").replace(":", "_") + ".zip";
                    
                    Map<String, Object> createDataResourceInMap = FastMap.newInstance();
                    createDataResourceInMap.put("dataResourceName", dataResourceName);
                    createDataResourceInMap.put("dataResourceTypeId", "URL_RESOURCE");
                    createDataResourceInMap.put("isPublic", "Y");
                    createDataResourceInMap.put("mimeTypeId", "application/zip");
                    createDataResourceInMap.put("objectInfo", "file://" + outFile.getAbsolutePath());
                    createDataResourceInMap.put("userLogin", sysUserLogin);
                    Map<String, Object> createDataResourceResults = dispatcher.runSync("createDataResource", createDataResourceInMap);
                    String dataResourceId = UtilGenerics.cast(createDataResourceResults.get("dataResourceId"));

                    Map<String, Object> createContentInMap = FastMap.newInstance();
                    createContentInMap.put("contentName", contentName);
                    createContentInMap.put("dataResourceId", dataResourceId);
                    createContentInMap.put("userLogin", sysUserLogin);
                    Map<String, Object> createContentResults = dispatcher.runSync("createContent", createContentInMap);
                    String contentId = UtilGenerics.cast(createContentResults.get("contentId"));
                    
                    Map<String, Object> createPartyContentInMap = FastMap.newInstance();
                    createPartyContentInMap.put("partyId", partyId);
                    createPartyContentInMap.put("contentId", contentId);
                    createPartyContentInMap.put("partyContentTypeId", "TENANT_BACKUP");
                    createPartyContentInMap.put("userLogin", sysUserLogin);
                    dispatcher.runSync("createPartyContent", createPartyContentInMap);
                }
                
                return ServiceUtil.returnSuccess(contentName + " has already been backed up.");
            } else {
                return ServiceUtil.returnError(firstResult);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * Create tenant backup
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteTenantBackup(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");
        
        try {
            List<EntityCondition> conds = FastList.newInstance();
            conds.add(EntityCondition.makeCondition("contentId", contentId));
            conds.add(EntityCondition.makeCondition("partyContentTypeId", "TENANT_BACKUP"));
            conds.add(EntityUtil.getFilterByDateExpr());
            List<GenericValue> partyContents = delegator.findList("PartyContent", EntityCondition.makeCondition(conds), null, null, null, false);
            if (UtilValidate.isNotEmpty(partyContents)) {
                GenericValue partyContent = EntityUtil.getFirst(partyContents);
                partyContent.set("thruDate", UtilDateTime.nowTimestamp());
                partyContent.store();
                
                // delete file
                GenericValue content = partyContent.getRelatedOne("Content", false);
                GenericValue dataResource = content.getRelatedOne("DataResource", false);
                if (UtilValidate.isNotEmpty(dataResource)) {
                    String contentName = content.getString("contentName");
                    String objectInfo = dataResource.getString("objectInfo");
                    URI uri = new URI(objectInfo);
                    File file = new File(uri);
                    file.delete();
                    return ServiceUtil.returnSuccess(contentName + " has already been deleted.");
                } else {
                    return ServiceUtil.returnError("Cound not find data resource of content: " + contentId);
                }
            } else {
                return ServiceUtil.returnError("Cound not find party content: " + contentId);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * Create tenant backup
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> restoreTenantBackup(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");
        
        try {
            List<EntityCondition> conds = FastList.newInstance();
            conds.add(EntityCondition.makeCondition("contentId", contentId));
            conds.add(EntityCondition.makeCondition("partyContentTypeId", "TENANT_BACKUP"));
            conds.add(EntityUtil.getFilterByDateExpr());
            List<GenericValue> partyContents = delegator.findList("PartyContent", EntityCondition.makeCondition(conds), null, null, null, false);
            if (UtilValidate.isNotEmpty(partyContents)) {
                GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
                GenericValue dataResource = content.getRelatedOne("DataResource", false);
                if (UtilValidate.isNotEmpty(dataResource)) {
                    String contentName = content.getString("contentName");
                    String dataResourceName = content.getString("dataResourceName");
                    String objectInfo = dataResource.getString("objectInfo");
                    URI uri = new URI(objectInfo);
                    File file = new File(uri);
                    
                    File tempDir = new File(file.getParentFile(), "tmpext_" + dataResourceName);
                    if(!tempDir.exists()){
                        tempDir.mkdir();
                    }
                    
                    //TODO:
                    //http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
                    
                    return ServiceUtil.returnSuccess(contentName + " has already been deleted.");
                } else {
                    return ServiceUtil.returnError("Cound not find data resource of content: " + contentId);
                }
            } else {
                return ServiceUtil.returnError("Cound not find party content: " + contentId);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * install tenant data sources
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> installTenantDataSources(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String readers = (String) context.get("readers");
        String newReaders = null;
        String files = (String) context.get("files");
        
        try {
            // check if the tenant is used as demo
            String isDemo = EntityUtilProperties.getPropertyValue("tenant", "isDemo", "Y", delegator);
            if ("Y".equals(isDemo) && UtilValidate.isEmpty(readers)
                    && UtilValidate.isEmpty(files) && UtilValidate.isEmpty(delegator.getDelegatorTenantId())) {
                // get a readers from the first componentDemo.properties file
                List<GenericValue> tenantComponents = delegator.findList("TenantComponent", EntityCondition.makeCondition("tenantId", tenantId), null, UtilMisc.toList("sequenceNum"), null, false);
                if (UtilValidate.isNotEmpty(tenantComponents)) {
                    GenericValue tenantComponent = EntityUtil.getFirst(tenantComponents);
                    String componentName = tenantComponent.getString("componentName");
                    readers = EntityUtilProperties.getPropertyValue(componentName + "Demo", "demoLoadData", delegator);
                }
                if (UtilValidate.isEmpty(readers)) {
                    readers = "security,seed,seed-initial,demo,ext,ext-demo,ext-test";  // load everything when not specified
                }
            } else if (UtilValidate.isEmpty(readers)
                    && UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) { // load only 'seed' if no readers but tenant exists
                readers = "seed";
            }
            
            // if the reader or files exists then install data
            if (UtilValidate.isNotEmpty(readers) || UtilValidate.isNotEmpty(files)) {
                if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                    TransactionUtil.commit();
                }
                
                // load data
                String configFile = FileUtil.getFile("component://base/config/ofbiz-containers.xml").getAbsolutePath();
                String delegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
                List<String> argList = FastList.newInstance();
                argList.add("-delegator=" + delegatorName);
                if (UtilValidate.isNotEmpty(readers)) {
                    argList.add("-readers=" + readers);
                }
                if (UtilValidate.isNotEmpty(files)) {
                    argList.add("-file=" + files);
                }
                String[] args = argList.toArray(new String[argList.size()]);
                EntityDataLoadContainer entityDataLoadContainer = new EntityDataLoadContainer();
                entityDataLoadContainer.init(args, "dataload-container", configFile);
                entityDataLoadContainer.start();
            }
        } catch (Exception e) {
            String errMsg = "Could not install databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            // do not return an error because it will block other correct tenants
            //return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * create tenant data source database
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> createTenantDataSourceDb(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String entityGroupName = (String) context.get("entityGroupName");
        try {
            TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
            connectionHandler.createDatabase();
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("isExist", connectionHandler.isExist());
            return results;
        } catch (Exception e) {
            String errMsg = "Could not install databases for tenant '" + tenantId + "' with entity group name '" + entityGroupName + "' : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }
    
    /**
     * delete tenant data source database
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteTenantDataSourceDb(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String entityGroupName = (String) context.get("entityGroupName");
        
        try {
            TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
            connectionHandler.deleteDatabase();
        } catch (Exception e) {
            String errMsg = "Could not delete a database for tenant " + tenantId + " with entity group name : " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    /**
     * delete tenant data source databases
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteTenantDataSourceDbs(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        
        try {
            List<GenericValue> tenantDataSources = delegator.findByAnd("TenantDataSource", UtilMisc.toMap("tenantId", tenantId), null, false);
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                
                // delete tenant data source
                Map<String, Object> installTenantDataSourceInMap = FastMap.newInstance();
                installTenantDataSourceInMap.put("tenantId", tenantId);
                installTenantDataSourceInMap.put("entityGroupName", entityGroupName);
                installTenantDataSourceInMap.put("userLogin", userLogin);
                Map<String, Object> results = dispatcher.runSync("deleteTenantDataSource", installTenantDataSourceInMap);
                if (ServiceUtil.isError(results)) {
                    List<String> errorMessageList = UtilGenerics.cast(results.get("errorMessageList"));
                    return ServiceUtil.returnError(errorMessageList);
                }
            }
            
            List<String> errMsgs = FastList.newInstance();
            if (UtilValidate.isNotEmpty(errMsgs)) {
                return ServiceUtil.returnError(errMsgs);
            }
        } catch (Exception e) {
            String errMsg = "Could not delete databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> recreateTenantDataSourceDbs(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");

        try {
            TenantJdbcConnectionHandler orgOfbizConnectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, "org.ofbiz", delegator);
            TenantJdbcConnectionHandler orgOfbizOlapConnectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, "org.ofbiz.olap", delegator);
            
            // delete databases
            orgOfbizConnectionHandler.deleteDatabase();
            orgOfbizOlapConnectionHandler.deleteDatabase();
            
            // create databases
            orgOfbizConnectionHandler.createDatabase();
            orgOfbizOlapConnectionHandler.createDatabase();

            // load data
            String configFile = FileUtil.getFile("component://base/config/ofbiz-containers.xml").getAbsolutePath();
            String delegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
            List<String> argList = FastList.newInstance();
            argList.add("-delegator=" + delegatorName);
            String[] args = argList.toArray(new String[argList.size()]);
            EntityDataLoadContainer entityDataLoadContainer = new EntityDataLoadContainer();
            entityDataLoadContainer.init(args, "dataload-container", configFile);
            entityDataLoadContainer.start();

            return ServiceUtil.returnSuccess();
        } catch (Exception e) {
            String errMsg = "Could not delete databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }

    /**
     * create user login for tenant
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> createUserLoginForTenant(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        
        Map<String, Object> toContext = FastMap.newInstance();
        
        // set createUserLogin service fields
        String serviceName = "createUserLogin";
        Map<String, Object> setServiceFieldsResults = TenantUtil.setServiceFields(serviceName, context, toContext, timeZone, locale, dispatcher);
        
        if (!ServiceUtil.isError(setServiceFieldsResults)) {
            // run createUserLogin service
            try {
                Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
                runTenantServiceInMap.put("tenantId", tenantId);
                runTenantServiceInMap.put("serviceName", serviceName);
                runTenantServiceInMap.put("serviceParameters", toContext);
                runTenantServiceInMap.put("isAsync", Boolean.FALSE);
                runTenantServiceInMap.put("userLogin", userLogin);
                Map<String, Object> results = dispatcher.runSync("runTenantService", runTenantServiceInMap);
                Map<String, Object> serviceResults = UtilGenerics.cast(results.get("serviceResults"));
                return serviceResults;
            } catch (Exception e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            return setServiceFieldsResults;
        }
    }

    /**
     * remove user login to security group for tenant
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> removeUserLoginFromSecurityGroupForTenant(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        
        Map<String, Object> toContext = FastMap.newInstance();
        
        // set removeUserLoginFromSecurityGroup service fields
        String serviceName = "removeUserLoginFromSecurityGroup";
        Map<String, Object> setServiceFieldsResults = TenantUtil.setServiceFields(serviceName, context, toContext, timeZone, locale, dispatcher);
        
        if (!ServiceUtil.isError(setServiceFieldsResults)) {
            // run removeUserLoginFromSecurityGroup service
            try {
                Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
                runTenantServiceInMap.put("tenantId", tenantId);
                runTenantServiceInMap.put("serviceName", serviceName);
                runTenantServiceInMap.put("serviceParameters", toContext);
                runTenantServiceInMap.put("isAsync", Boolean.FALSE);
                runTenantServiceInMap.put("userLogin", userLogin);
                Map<String, Object> results = dispatcher.runSync("runTenantService", runTenantServiceInMap);
                Map<String, Object> serviceResults = UtilGenerics.cast(results.get("serviceResults"));
                return serviceResults;
            } catch (Exception e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            return setServiceFieldsResults;
        }
    }

    /**
     * update user login to security group for tenant
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> updateUserLoginToSecurityGroupForTenant(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String tenantId = (String) context.get("tenantId");
        
        Map<String, Object> toContext = FastMap.newInstance();
        // set updateUserLoginToSecurityGroup service fields
        String serviceName = "updateUserLoginToSecurityGroup";
        Map<String, Object> setServiceFieldsResults = TenantUtil.setServiceFields(serviceName, context, toContext, timeZone, locale, dispatcher);
        
        if (!ServiceUtil.isError(setServiceFieldsResults)) {
            // run updateUserLoginToSecurityGroup service
            try {
                Map<String, Object> runTenantServiceInMap = FastMap.newInstance();
                runTenantServiceInMap.put("tenantId", tenantId);
                runTenantServiceInMap.put("serviceName", serviceName);
                runTenantServiceInMap.put("serviceParameters", toContext);
                runTenantServiceInMap.put("isAsync", Boolean.FALSE);
                runTenantServiceInMap.put("userLogin", userLogin);
                Map<String, Object> results = dispatcher.runSync("runTenantService", runTenantServiceInMap);
                Map<String, Object> serviceResults = UtilGenerics.cast(results.get("serviceResults"));
                return serviceResults;
            } catch (Exception e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            return setServiceFieldsResults;
        }
    }

    /**
     * run a service for a specific tenant
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> runTenantService(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        
        String tenantId = (String) context.get("tenantId");
        String serviceName = (String) context.get("serviceName");
        Map<String, Object> serviceParameters = UtilGenerics.cast(context.get("serviceParameters"));
        Boolean isAsync = (Boolean) context.get("isAsync");
        if (UtilValidate.isEmpty(isAsync)) {
            isAsync = Boolean.FALSE;
        }
        
        try {
            String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
            String tenantDispatcherName = dispatcher.getName() + "#" + tenantId;
            Delegator tenantDelegator = DelegatorFactory.getDelegator(tenantDelegatorName);
            LocalDispatcher tenantDispatcher = ServiceContainer.getLocalDispatcher(tenantDispatcherName, tenantDelegator);
            
            Map<String, Object> serviceResults = null;
            if (isAsync) {
                tenantDispatcher.runAsyncWait(serviceName, serviceParameters);
                serviceResults = ServiceUtil.returnSuccess();
            } else {
                serviceResults = tenantDispatcher.runSync(serviceName, serviceParameters);
            }
            
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("serviceResults", serviceResults);
            return results;
        } catch (GenericServiceException e) {
            String errMsg = "Could not run service [" + serviceName + "] for tenant [" + tenantId + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            Map<String, Object> results = ServiceUtil.returnError(errMsg);
            Map<String, Object> serviceResults = ServiceUtil.returnError(errMsg);
            results.put("serviceResults", serviceResults);
            return results;
        }
    }
    
    /**
     * Get Tenant By UserLogin
     * @param ctx
     * @param context
     * @return String tenantId
     */
    public static Map<String, Object> getTenantIdByUserLoginId(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        String userLoginId = (String) context.get("userLoginId");
        Map<String,Object> result = ServiceUtil.returnSuccess();
        String tenantId = null;
        
        // retrieve Tenant data
        try {
            if (UtilValidate.isNotEmpty(userLoginId)) {
                GenericValue userPreference = delegator.findOne("UserPreference", UtilMisc.toMap("userLoginId", userLoginId, "userPrefTypeId", "TENANT"), false);
                if (UtilValidate.isNotEmpty(userPreference)) {
                    tenantId = userPreference.getString("userPrefValue");
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage("PrefErrorUiLabels", "setPreference.invalidArgument", locale));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        
        result.put("tenantId", tenantId);
        return result;
    }
    
    /**
     * Get UserLogin By Tenant
     * @param ctx
     * @param context
     * @return String userLoginId
     */
    public static Map<String, Object> getUserLoginIdByTenantId(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        Map<String,Object> result = ServiceUtil.returnSuccess();
        String userLoginId = null;
        
        // retrieve userLogin data
        try {
            if (UtilValidate.isNotEmpty(tenantId)) {
                List<GenericValue> userPreferenceList = delegator.findByAnd("UserPreference", UtilMisc.toMap("userPrefTypeId", "TENANT","userPrefValue", tenantId, "userPrefGroupTypeId", "GLOBAL_PREFERENCES"), null, false);
                if (UtilValidate.isNotEmpty(userPreferenceList)) {
                    GenericValue userPreference = EntityUtil.getFirst(userPreferenceList);
                    userLoginId = userPreference.getString("userLoginId");
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage("PrefErrorUiLabels", "setPreference.invalidArgument", locale));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        
        result.put("userLoginId", userLoginId);
        return result;
    }
}

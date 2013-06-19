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
import java.util.List;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tenant Container
 * @author chatree
 *
 */
public class TenantContainer implements Container {
    
    public final static String module = TenantContainer.class.getName();

    public final static String DEFAULT_TENANT_ID = "default";
    public final static String DEFAULT_TENANT_FILE_NAME = "default-tenant.xml";
    
    protected String name = null;
    protected String configFile = null;

    public void init(String[] args, String name, String configFile)
            throws ContainerException {
        this.name = name;
        this.configFile = configFile;
    }

    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer("tenant-container", configFile);
        String delegatorName = ContainerConfig.getPropertyValue(cfg, "delegator-name", "default");
        Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
        
        try {
            String ofbizHome = System.getProperty("ofbiz.home");
            File defaultTenantFile = FileUtil.getFile(new File(ofbizHome), "hot-deploy/tenant/config/default-tenant.xml");
            if (defaultTenantFile.exists()) {
                GenericValue defaultTenant = delegator.findOne("Tenant", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID), false);
                if (UtilValidate.isEmpty(defaultTenant)) {
                    // create tenant
                    defaultTenant = delegator.makeValue("Tenant", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID, "tenantName", DEFAULT_TENANT_ID));
                    delegator.create(defaultTenant);
                    
                    // create tenant data sources
                    GenericHelperInfo mainHelperInfo = delegator.getGroupHelperInfo("org.ofbiz");
                    GenericHelperInfo olapHelperInfo = delegator.getGroupHelperInfo("org.ofbiz.olap");
                    Datasource mainDatasource = EntityConfigUtil.getDatasource(mainHelperInfo.getHelperBaseName());
                    Datasource olapDatasource = EntityConfigUtil.getDatasource(olapHelperInfo.getHelperBaseName());
                    String mainJdbcUri = mainDatasource.inlineJdbc.getJdbcUri();
                    String mainJdbcUsername = mainDatasource.inlineJdbc.getJdbcUsername();
                    String mainJdbcPassword = mainDatasource.inlineJdbc.getJdbcPassword();
                    String olapJdbcUri = olapDatasource.inlineJdbc.getJdbcUri();
                    String olapJdbcUsername = olapDatasource.inlineJdbc.getJdbcUsername();
                    String olapJdbcPassword = olapDatasource.inlineJdbc.getJdbcPassword();
                    GenericValue mainTenantDataSource = delegator.makeValue("TenantDataSource", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID
                            , "entityGroupName", "org.ofbiz" , "jdbcUri", mainJdbcUri, "jdbcUsername", mainJdbcUsername, "jdbcPassword", mainJdbcPassword));
                    GenericValue olapTenantDataSource = delegator.makeValue("TenantDataSource", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID
                            , "entityGroupName", "org.ofbiz.olap" , "jdbcUri", olapJdbcUri, "jdbcUsername", olapJdbcUsername, "jdbcPassword", olapJdbcPassword));
                    delegator.create(mainTenantDataSource);
                    delegator.create(olapTenantDataSource);
        
                    // create tenant components
                    List<String> defaultTenantComponentNames = ComponentConfig.getDefaultTenantComponentNames();
                    for (String defaultTenantComponentName : defaultTenantComponentNames) {
                        GenericValue tenantComponent = delegator.makeValue("TenantComponent", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID, "componentName", defaultTenantComponentName));
                        delegator.create(tenantComponent);
                    }
                    
                    // create tenant domain names
                    Document document = UtilXml.readXmlDocument(defaultTenantFile.toURI().toURL());
                    List<? extends Element> domainElements = UtilXml.childElementList(document.getDocumentElement(), "domain");
                    for (Element domainElement : domainElements) {
                        String name = domainElement.getAttribute("name");
                        String initialPath = domainElement.getAttribute("initial-path");
                        GenericValue tenantDomainName = delegator.makeValue("TenantDomainName", UtilMisc.toMap("tenantId", DEFAULT_TENANT_ID
                                , "domainName", name, "initialPath", initialPath));
                        delegator.create(tenantDomainName);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, module);
        }
        
        return false;
    }

    public void stop() throws ContainerException {

    }

    public String getName() {
        return name;
    }

}

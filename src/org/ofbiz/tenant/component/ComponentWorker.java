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
package org.ofbiz.tenant.component;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.util.EntityDataLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Component Worker
 * @author chatree
 *
 */
public class ComponentWorker {

    public final static String module = ComponentWorker.class.getName();
    
    /**
     * get framework component names
     * @return
     * @throws Exception
     */
    public static List<String> getFrameworkComponentNames() throws Exception {
        String ofbizHome = System.getProperty("ofbiz.home");
        File componentLoadFile = new File(ofbizHome, "framework" + File.separatorChar + "component-load.xml");
        Document document = UtilXml.readXmlDocument(componentLoadFile.toURI().toURL());
        List<? extends Element> elements = UtilXml.childElementList(document.getDocumentElement());
        List<String> componentNames = FastList.newInstance();
        for (Element element : elements) {
            String componentName = UtilXml.elementAttribute(element, "component-location", null);
            componentNames.add(componentName);
        }
        return componentNames;
    }
    
    /**
     * get applications component names
     * @return
     * @throws Exception
     */
    public static List<String> getApplicationsComponentNames() throws Exception {
        String ofbizHome = System.getProperty("ofbiz.home");
        File componentLoadFile = new File(ofbizHome, "applications" + File.separatorChar + "component-load.xml");
        Document document = UtilXml.readXmlDocument(componentLoadFile.toURI().toURL());
        List<? extends Element> elements = UtilXml.childElementList(document.getDocumentElement());
        List<String> componentNames = FastList.newInstance();
        for (Element element : elements) {
            String componentName = UtilXml.elementAttribute(element, "component-location", null);
            componentNames.add(componentName);
        }
        return componentNames;
    }
    
    /**
     * get themes component names
     * @return
     * @throws Exception
     */
    public static List<String> getThemesComponentNames() throws Exception {
        String ofbizHome = System.getProperty("ofbiz.home");
        File themesDir = new File(ofbizHome, "themes");
        List<String> componentNames = FastList.newInstance();
        if (themesDir.isDirectory()) {
            File[] themeDirs = themesDir.listFiles(new FileFilter() {
                
                @Override
                public boolean accept(File file) {
                    File dataDir = new File(file, "data");
                    return dataDir.exists();
                }
            });
            
            for (File themeDir : themeDirs) {
                String themeName = themeDir.getName();
                componentNames.add(themeName);
            }
        }
        return componentNames;
    }
    
    /**
     * get specialpurpose component names
     * @return
     * @throws Exception
     */
    public static List<String> getSpecialPurposeComponentNames() throws Exception {
        String ofbizHome = System.getProperty("ofbiz.home");
        File componentLoadFile = new File(ofbizHome, "specialpurpose" + File.separatorChar + "component-load.xml");
        Document document = UtilXml.readXmlDocument(componentLoadFile.toURI().toURL());
        List<? extends Element> elements = UtilXml.childElementList(document.getDocumentElement());
        List<String> componentNames = FastList.newInstance();
        for (Element element : elements) {
            String componentName = UtilXml.elementAttribute(element, "component-location", null);
            componentNames.add(componentName);
        }
        return componentNames;
    }
    
    /**
     * get entity data URL string by component list
     * @param entityGroupName
     * @param components
     * @param readers
     * @param delegator
     * @return
     */
    public static String getEntityDataUrlStringByComponentList(String entityGroupName, List<String> components, String readers, Delegator delegator) {
        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroupName);
        String helperName = helperInfo.getHelperBaseName();
        List<String> readerNames = StringUtil.split(readers, ",");
        
        // get URL list from the component list
        List<URL> urls = null;
        if (UtilValidate.isNotEmpty(readerNames)) {
            urls = EntityDataLoader.getUrlByComponentList(helperName, components, readerNames);
        } else {
            urls = EntityDataLoader.getUrlByComponentList(helperName, components);
        }
        
        // build a String from the list of URL
        StringBuilder filesBuilder = new StringBuilder();
        for (URL url : urls) {
            filesBuilder.append(url.toString() + ",");
        }
        
        //get rid of the last comma
        String files = filesBuilder.toString().substring(0, filesBuilder.toString().length() - 1);
        
        return files;
    }
}

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
package org.ofbiz.tenant.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilIO;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;

public class TenantImageFilter implements Filter {
    
    public final static String module = TenantImageFilter.class.getName();

    protected FilterConfig config = null;
    
    @Override
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        Delegator delegator = (Delegator) config.getServletContext().getAttribute("delegator");
        if (UtilValidate.isNotEmpty(delegator) && UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) {
            String tenantId = delegator.getDelegatorTenantId();
            String tenantRuntimePath = System.getProperty("ofbiz.home") + File.separatorChar + "runtime" + File.separatorChar + "tenants" + File.separatorChar + tenantId;
            String path = tenantRuntimePath + httpRequest.getRequestURI();
            try {
                File inputFile = FileUtil.getFile(path);
                FileInputStream fis = new FileInputStream(inputFile);
                UtilIO.copy(fis, true, response.getOutputStream(), false);
            } catch (FileNotFoundException e) {
                httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public void destroy() {
        
    }

}

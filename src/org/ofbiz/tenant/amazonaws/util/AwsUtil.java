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
package org.ofbiz.tenant.amazonaws.util;

import org.ofbiz.base.util.GeneralException;

import com.amazonaws.services.route53.model.RRType;

public class AwsUtil {

    public final static String module = AwsUtil.class.getName();
    
    /**
     * get resource record type
     * @param value
     * @return
     * @throws GeneralException
     */
    public static RRType getRRType(String value) throws GeneralException {
        RRType rrType = null;
        if ("A".equals(value)) {
            rrType = RRType.A;
        } else if ("CNAME".equals(value)) {
            rrType = RRType.CNAME;
        } else if ("MX".equals(value)) {
            rrType = RRType.MX;
        } else if ("AAAA".equals(value)) {
            rrType = RRType.AAAA;
        } else if ("TXT".equals(value)) {
            rrType = RRType.TXT;
        } else if ("PTR".equals(value)) {
            rrType = RRType.PTR;
        } else if ("SRV".equals(value)) {
            rrType = RRType.SRV;
        } else if ("SPF".equals(value)) {
            rrType = RRType.SPF;
        } else if ("NS".equals(value)) {
            rrType = RRType.NS;
        } else if ("SOA".equals(value)) {
            rrType = RRType.SOA;
        } else {
            throw new GeneralException("The type need to be one of: A, CNAME, MX, AAAA, TXT, PTR, SRV, SPF, NS, SOA");
        }
        return rrType;
    }
}

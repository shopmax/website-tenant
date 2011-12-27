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

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;

public class AwsFactory {
    
    public final static String module = AwsFactory.class.getName();
    
    public final static String CREDENTIALS_PROPERTIES = "AwsCredentials.properties";
    
    private static AWSCredentials credentials = null;
    private static AmazonEC2 ec2 = null;
    private static AmazonRoute53 route53 = null;
    
    /**
     * get AWS credentials
     * @return
     */
    public static AWSCredentials getAWSCredentials() {
        if (UtilValidate.isEmpty(credentials)) {
            String accessKey = UtilProperties.getPropertyValue(CREDENTIALS_PROPERTIES, "accessKey");
            String secretKey = UtilProperties.getPropertyValue(CREDENTIALS_PROPERTIES, "secretKey");
            credentials = new BasicAWSCredentials(accessKey, secretKey);
        }
        return credentials;
    }
    
    /**
     * get Amazon EC2
     * @return
     */
    public static AmazonEC2 getAmazonEC2() {
        if (UtilValidate.isEmpty(ec2)) {
            AWSCredentials credentials = getAWSCredentials();
            ec2 = new AmazonEC2Client(credentials);
        }
        return ec2;
    }
    
    /**
     * get Amazon Route53
     * @return
     */
    public static AmazonRoute53 getAmazonRoute53() {
        if (UtilValidate.isEmpty(route53)) {
            AWSCredentials credentials = getAWSCredentials();
            route53 = new AmazonRoute53Client(credentials);
        }
        return route53;
    }
}

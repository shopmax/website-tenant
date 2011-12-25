to automatically create a tenant demo systems:

In the component itself:
1. create data/TenantData.xml

example: (the records should be in this order:

<?xml version="1.0" encoding="UTF-8"?>
<entity-engine-xml>
    <Tenant tenantId="TENANTID" tenantName="Tenant Name" domainName="anyname.example.com" initialPath="/componentname"/>
    <TenantComponent tenantId="TENANTID" componentName="componentname" sequenceNum="01"/>
    <TenantDataSource tenantId="TENANTID" entityGroupName="org.ofbiz"
        jdbcUri="jdbc:postgresql://127.0.0.1/demoTIDofbiz" jdbcUsername="ofbiz" jdbcpassword="ofbiz"/>
    <TenantDataSource tenantId="TENANTID" entityGroupName="org.ofbiz.olap"
        jdbcUri="jdbc:postgresql://127.0.0.1/demoTIDolap" jdbcUsername="ofbiz" jdbcPassword="ofbiz"/>
</entity-engine-xml>


2. add this entry to the ofbiz-component.xml file:
 <entity-resource type="data" reader-name="tenant" loader="main" location="data/TenantData.xml"/>
 
3. add a file with the name:  DemoLoadData.txt:
content:
seed,seed-initial,demo,ext-demo

to specify what needs to be loaded for this tenant.

4. create basic databases
ofbiz
ofbizolap
ofbiztenant

5. run the following command to create all required databases and content:
./ant run-install-readers -Ddata-readers=seed,seed-initial,tenant,demo,ext-demo

the readers spec here is for the basic system only.


===============================================================
Test Link Util 0.0.1
===============================================================

Contents

1. Introduction
2. Installation
3. Bug Reports and Feedback


1. Introduction
===============================================================
TestLink is a web based test management and test execution system.
It enables quality assurance teams to create and manage their test 
cases as well as to organize them into test plans. These test plans 
allow team members to execute test cases and track test results 
dynamically.

TestLinkUtil is a application which enables integration of test cases 
written in TestNG with Test Link based in TestLink ID. The test case results can be directly updated 
from execution of test cases using eclipse/jenkins/command line.

More Details at  
https://wiki.vip.corp.ebay.com/display/ICOEQE/4.+Test+Case+Id+based+-+MAUI+Integration+with+TestLink

2. Installation
===============================================================
Add following repositories to POM.xml


        <repository>
            <id>tradingqe.releases</id>
            <name>tradingqe releases repo</name>
            <url>http://nxrepository.corp.ebay.com/nexus/content/repositories/ebay.tradingqe.releases</url>
            <layout>default</layout>

            <snapshots>
                <enabled>false</enabled>

            </snapshots>
            <releases>
                <enabled>true</enabled>
            </releases>
        </repository>
        <repository>
            <id>tradingqe.snapshots</id>
            <name>tradingqe snapshots repo</name>
            <url>http://nxrepository.corp.ebay.com/nexus/content/repositories/ebay.tradingqe.snapshots</url>
            <layout>default</layout>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
        
Add following dependency to POM.xml


        <dependency>
            <groupId>com.ebay</groupId>
            <artifactId>TestLinkUtil</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
 
 More Details at 
 https://wiki.vip.corp.ebay.com/display/ICOEQE/TestLink+and+TAPe+Framework+integration
 
 3. Bug Reports and Feedback
===============================================================
 Please send your feedback to apannasa@ebay.com,mohanbn@ebay.com
 

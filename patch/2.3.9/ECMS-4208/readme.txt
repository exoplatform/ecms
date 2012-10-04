Summary: Issue title Markup Cache is never used in ECMS
CCP Issue: N/A
Product Jira Issue: ECMS-4208.
Complexity: Normal
Proposal
 
Problem description
What is the problem to fix?

  * Markup Cache is never used in ECMS. 
  * Each time a front-end page is rendered, WCMComposerImpl.getContent() is always called. It means data is not cached and JCR access is always * performed.
  * This happens only for logged in user.

Fix description
Problem analysis

  * Portal team changed their implementation of FutureCache so our PortletFutureCache which extends FutureCache does not work correctly

How is the problem fixed?

  * Replace PortletFutureCache by FutureExoCache which also extends FutureCache for Markup Cache

Tests to perform
Reproduction test

  * In method WCMComposerImpl.getContent(), add this line:
  * System.out.println("Get content for: " + getRemoteUser() + "; node:" + nodeIdentifier);
  * Access ecmdemo/acme and press F5 many times.
  * Expected result: Get content for is displayed in console only after each 30 seconds (expiration time for Markup Cache)
  * Actual result: Get content for is always displayed -> Cache does not work -> KO

Tests performed at DevLevel

cf Aboves
Tests performed at Support Level

cf Aboves
Tests performed at QA

cf aboves

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests

*

Changes in Selenium scripts 

*
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

  * No
Configuration changes

Configuration changes:

In configuration.properties, add configuration for Markup Cache:
   
  *  wcm.cache.fragmentcacheservice.capacity=10000
  *  wcm.cache.fragmentcacheservice.timetolive=30
    
  In core/core-configuration/src/main/webapp/WEB-INF/conf/wcm-core/core-services-configuration.xml, add configuration for ExoCache:

  <external-component-plugins>
    <target-component>org.exoplatform.services.cache.CacheService</target-component>
    <component-plugin>
      <name>addExoCacheConfig</name>
      <set-method>addExoCacheConfig</set-method>
      <type>org.exoplatform.services.cache.ExoCacheConfigPlugin</type>
      <description>Configures the cache for Template Service</description>
      <init-params>
        <! Markup Cache >
        <object-param>
           <name>cache.config.FragmentCacheService</name>
           <description></description>
           <object type="org.exoplatform.services.cache.ExoCacheConfig">
           <field name="name"><string>FragmentCacheService</string></field>
            <field name="maxSize"><int>${wcm.cache.fragmentcacheservice.capacity:10000}</int></field>
            <field name="liveTime"><long>${wcm.cache.fragmentcacheservice.timetolive:30}</long></field>
            <field name="implementation"><string>org.exoplatform.services.cache.concurrent.ConcurrentFIFOExoCache</string></field>
          </object>
        </object-param>
      </init-params>
    </component-plugin>
   </external-component-plugins>

Will previous configuration continue to work?

  * Yes
Risks and impacts
  * Can this bug fix have any side effects on current client projects?

Any change in API (name, signature, annotation of a class/method)? No
  * Data (template, node type) migration/upgrade: No

Is there a performance risk/cost?

  * No

Validation (PM/Support/QA)
PM Comment

*
Support Comment

*
QA Feedbacks

*

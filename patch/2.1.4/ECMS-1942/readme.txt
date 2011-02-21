Summary

    * Status: Problem with PortletFutureCache
    * CCP Issue: CCP-758, Product Jira Issue: ECMS-1942.
    * Complexity: middle

The Proposal
Problem description

What is the problem to fix?

An OutOfMemory error occurs after the website being indexed by Google web spider.
An analysis of the memory shows that PortletFutureCache takes an abnormaly high amount of memory

The detailed steps:
The issue requires production environment (such as CWI) with a lot of content and a spider tool

   1. the TestPortletFutureCache.testCacheEvictions() ensures the eviction works well

Fix description

    * Fixed eviction algorithm
    * Exposed the FragmentCacheService on JMX and REST management interfaces
    * default cache size was set to 5000
    * introduced cache-size init-param in FragmentCacheService

Patch file: ECMS-1942.patch

Tests to perform

Reproduction test

    * See problem description

Tests performed at DevLevel

    * TestPortletFutureCache
    * TestFragmentCacheService

Documentation changes

Documentation changes:

    * the init-params cleanup-cache and cache-size of FragmentCacheService should be documented in the refguide
    * the expirationCache properties-param of WCMServiceImpl

      <component>
        <key>org.exoplatform.services.wcm.core.WCMService</key>
        <type>org.exoplatform.services.wcm.core.impl.WCMServiceImpl</type>
        <init-params>
          <properties-param>
            <name>server.config</name>
            <description>server.config</description>
            <property name="expirationCache" value="30" />
          </properties-param>
        </init-params>
      </component>

Configuration changes

Configuration changes:

Will previous configuration continue to work?

    * yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

Is there a performance risk/cost?
* N/A

Validation (PM/Support/QA)

PM Comment
*

Support Comment
The builtin config is not appropriate because the cache eviction thread runs every 5 minutes (cleanup-cache) for a maximum retention of the markup of 30 seconds (expirationCache).

QA Feedbacks

The eviction thread should run more frequently than the cache expiration

Recommanded settings :

<component>
<key>org.exoplatform.services.portletcache.FragmentCacheService</key>
<type>org.exoplatform.services.portletcache.FragmentCacheService</type>
<init-params>
<value-param>
<name>cleanup-cache</name>
<description>The cleanup cache period in seconds</description>
<value>15</value>
</value-param>
<value-param>
<name>cache-size</name>
<description>cache size</description>
<value>10000</value>
</value-param>
</init-params>
</component>



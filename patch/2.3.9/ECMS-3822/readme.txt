Summary
  * Issue title WARNING: ==> Can not init action 'addToFavorite' and workspace 'collaboration'
  * CCP Issue: N/A
  * Product Jira Issue: ECMS-3822.

Complexity: Normal

Proposal
 

Problem description
What is the problem to fix?

  * Case1: At startup time, there is a warning:
    WARNING: ==> Can not init action 'addToFavorite' and workspace 'collaboration'

  * Case2: After startup server, we got an exception:
       INFO: Admin identity is read from configuration file
       4 sept. 2012 15:10:30 sun.reflect.NativeMethodAccessorImpl invoke0
       GRAVE: Add Favorite failed
       java.lang.NullPointerException: Cannot invoke method getIdentity() on null object
       at org.codehaus.groovy.runtime.NullObject.invokeMethod(NullObject.java:77)
       at org.codehaus.groovy.runtime.callsite.PogoMetaClassSite.call(PogoMetaClassSite.java:45)
       ...
  * Case3: got problem after create a space
       INFO: Admin identity is read from configuration file
       4 sept. 2012 15:10:30 sun.reflect.NativeMethodAccessorImpl invoke0
       GRAVE: Add Favorite failed
       java.lang.NullPointerException: Cannot invoke method getIdentity() on null object
       at org.codehaus.groovy.runtime.NullObject.invokeMethod(NullObject.java:77)
       at org.codehaus.groovy.runtime.callsite.PogoMetaClassSite.call(PogoMetaClassSite.java:45)
       at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:40)
       ...
  * Case 4: got the same exception as case 3 when leaving login timeout 
  * Case 5: a file is not marked as favorite automatically when uploaded to a favorite folder

Fix description

Problem analysis

How is the problem fixed?

   * In packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/dms-actions-configuration.xml, add field properies for action.
   * Check and do not trigger action for SYSTEM, ANONYMOUS or when ConversationState does not exist
   * Avoid getting Service directly from current container, instead of that use WCMCoreUtils.getService();

Tests to perform
Reproduction test

Tests performed at DevLevel

cf Aboves
Tests performed at Support Level

*
Tests performed at QA

*
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

*
Changes in Selenium scripts 

*
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

No
Configuration changes
Configuration changes:

   * In packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/dms-actions-configuration.xml, add field properies for action.

Will previous configuration continue to work?
  * Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
  * No
Data (template, node type) migration/upgrade: 
  * No
Is there a performance risk/cost?
  * No

Summary
JCR Session unclosed in LinkManagerImpl.getAllLinks()

CCP Issue: N/A
Product Jira Issue: ECMS-3692.
Complexity: Normal
Summary
Proposal
Problem description
Fix description
Tests to perform
Changes in Test Referential
Documentation changes
Configuration changes
Risks and impacts
Validation (PM/Support/QA)
This page should represent the synthesis of information known about the issue fix.
This information will be used to create the general release notes file.
eXo internal information

Impacted Client(s): No
Proposal
 

Problem description
What is the problem to fix?

Enable the Session Leak Detector as described here http://wiki.exoplatform.com/xwiki/bin/view/JCR/Session%20leak%20detector
Start server
Work in wiki (add some pages)
Stop the server and restart.
After the server restarts up, there's an exception of LinkManagerImpl.getAllLinks()
Apr 11, 2012 10:39:24 AM org.exoplatform.services.jcr.impl.core.SessionReference$1 run
SEVERE: null
java.lang.Exception
at org.exoplatform.services.jcr.impl.core.SessionReference.<init>(SessionReference.java:138)
at org.exoplatform.services.jcr.impl.core.TrackedSession.<init>(TrackedSession.java:33)
at org.exoplatform.services.jcr.impl.core.SessionFactory.createSession(SessionFactory.java:141)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:401)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:80)
at org.exoplatform.services.jcr.ext.common.SessionProvider.getSession(SessionProvider.java:193)
at org.exoplatform.services.cms.link.impl.LinkManagerImpl.getAllLinks(LinkManagerImpl.java:347)
at org.exoplatform.services.cms.impl.Utils.removeDeadSymlinksRecursively(Utils.java:350)
at org.exoplatform.services.cms.impl.Utils.removeDeadSymlinks(Utils.java:344)
at org.exoplatform.services.cms.jcrext.RemoveNodeAction.execute(RemoveNodeAction.java:50)
at org.exoplatform.services.jcr.impl.ext.action.SessionActionInterceptor.launch(SessionActionInterceptor.java:497)
at org.exoplatform.services.jcr.impl.ext.action.SessionActionInterceptor.preRemoveItem(SessionActionInterceptor.java:431)
at org.exoplatform.services.jcr.impl.core.ItemImpl.remove(ItemImpl.java:366)
at org.chromattic.core.jcr.SessionWrapperImpl.remove(SessionWrapperImpl.java:206)
Fix description
How is the problem fixed?

Add new function getAllLinks() for LinkManager service with SessionProvider in parameter list, replace old function getAllLinks() without SessionProvider by new function. So the session got from SessionProvider will be logged out automatically when the SessionProvider is closed.
Tests to perform
Reproduction test

steps ...
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

None
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

Add new method: LinkManager.getAllLinks(Node, String, SessionProvider):
https://jira.exoplatform.org/browse/DOC-1618
  
Configuration changes
Configuration changes:

No
Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: No
Data (template, node type) migration/upgrade: No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated
QA Feedbacks

N/A

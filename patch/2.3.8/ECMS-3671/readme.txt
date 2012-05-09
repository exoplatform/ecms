Summary
JCR Session unclosed in TemplateServiceImpl

CCP Issue: N/A
Product Jira Issue: ECMS-3660.
Product Jira Issue: ECMS-3671.
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
Enabled the Session Leak Detector as described here http://wiki.exoplatform.com/xwiki/bin/view/JCR/Session%20leak%20detector on PLF 3.5.1 and realized that there were several session leaks:

Case 1:  ECMS-3660: JCR Session unclosed in TemplateServiceImpl.addTemplate()
java.lang.Exception
at org.exoplatform.services.jcr.impl.core.SessionReference.<init>(SessionReference.java:138)
at org.exoplatform.services.jcr.impl.core.TrackedSession.<init>(TrackedSession.java:33)
at org.exoplatform.services.jcr.impl.core.SessionFactory.createSession(SessionFactory.java:141)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:401)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:80)
at org.exoplatform.services.jcr.ext.common.SessionProvider.getSession(SessionProvider.java:193)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.getSession(TemplateServiceImpl.java:853)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.addTemplate(TemplateServiceImpl.java:994)
at org.exoplatform.services.cms.templates.impl.TemplatePlugin.addNode(TemplatePlugin.java:339)
at org.exoplatform.services.cms.templates.impl.TemplatePlugin.addTemplate(TemplatePlugin.java:283)
Case 2: ECMS-3671: JCR Session unclosed in TemplateServiceImpl.getAllDocumentNodeTypes()
java.lang.Exception
at org.exoplatform.services.jcr.impl.core.SessionReference.<init>(SessionReference.java:138)
at org.exoplatform.services.jcr.impl.core.TrackedSession.<init>(TrackedSession.java:33)
at org.exoplatform.services.jcr.impl.core.SessionFactory.createSession(SessionFactory.java:141)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:401)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:80)
at org.exoplatform.services.jcr.ext.common.SessionProvider.getSession(SessionProvider.java:193)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.getSession(TemplateServiceImpl.java:853)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.getAllDocumentNodeTypes(TemplateServiceImpl.java:639)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.getDocumentTemplates(TemplateServiceImpl.java:575)
at org.exoplatform.services.cms.templates.impl.TemplateServiceImpl.start(TemplateServiceImpl.java:135)
Fix description
How is the problem fixed?

ECMS-3660: In TemplateServiceImpl class, do not create new session, use session of node given in method parameter list to perform data storing and retrievement
ECMS-3671: In method getAllDocumentNodeTypes() of TemplateServiceImpl class, Create new session from SessionProvider object and then close it explicitly.
Tests to perform
Reproduction test

cf. above
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

N/A
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

No
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

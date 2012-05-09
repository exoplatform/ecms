Summary
JCR Session unclosed in StorageProviderImpl.init()

CCP Issue: N/A
Product Jira Issue: ECMS-3661.
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
Enabled the Session Leak Detector as described here http://wiki.exoplatform.com/xwiki/bin/view/JCR/Session%20leak%20detector on PLF 3.5.1 and realized that there were several session leaks, one of them is in StorageProviderImpl.init() as shown in this stack trace provided by the leak detector

lang.Exception
at org.exoplatform.services.jcr.impl.core.SessionReference.<init>(SessionReference.java:138)
at org.exoplatform.services.jcr.impl.core.TrackedSession.<init>(TrackedSession.java:33)
at org.exoplatform.services.jcr.impl.core.SessionFactory.createSession(SessionFactory.java:141)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:401)
at org.exoplatform.services.jcr.impl.core.RepositoryImpl.getSystemSession(RepositoryImpl.java:80)
at org.exoplatform.ecms.xcmis.sp.StorageProviderImpl.init(StorageProviderImpl.java:285)
at org.exoplatform.ecms.xcmis.sp.DriveCmisRegistry.start(DriveCmisRegistry.java:272)
at sun.reflect.GeneratedMethodAccessor111.invoke(Unknown Source)
at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
Fix description
How is the problem fixed?

In method init() of StorageProviderImpl class, always logout session after initializing data
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

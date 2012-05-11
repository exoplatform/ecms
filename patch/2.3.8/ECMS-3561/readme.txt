Summary
[WARNINGS] wcm.WCMCoreUtils - Primary item not found, The content type: nt:unstructured isn't supported

CCP Issue: N/A
Product Jira Issue: ECMS-3561.
Complexity: N/A

Impacted Client(s): N/A 




Proposal
 

Problem description
What is the problem to fix?

Running cloud-workspaces.com 1.0.0-beta9 found following warnings in the log:
There is few problems:
wcm.WCMCoreUtils and UIDocumentInfo:
2012-03-07 18:11:15,809 [http-8080-42] INFO  org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryListener - User paul logged in.
2012-03-07 18:12:13,617 [http-8080-8] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private
2012-03-07 18:12:13,689 [http-8080-8] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private
2012-03-07 18:12:13,937 [http-8080-8] WARN currentTenant=snatzo org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo - The content type: nt:unstructured isn't supported by any template
2012-03-07 18:12:15,996 [http-8080-53] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private
2012-03-07 18:12:16,000 [http-8080-53] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private
2012-03-07 18:12:16,005 [http-8080-53] WARN currentTenant=snatzo org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo - The content type: nt:unstructured isn't supported by any template
2012-03-07 18:12:30,882 [http-8080-53] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private/Documents
2012-03-07 18:12:30,887 [http-8080-53] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private/Documents
wcm:PublicationUpdateStateListener:

2012-03-07 18:14:33,482 [http-8080-42] INFO currentTenant=snatzo org.exoplatform.services.wcm.publication.listener.post.PostCreateContentEventListener - /Users/p___/pa___/pau___/paul/Private/Pictures/test/Cloud desk.jpg::intranet::paul
2012-03-07 18:14:33,482 [http-8080-42] INFO currentTenant=snatzo org.exoplatform.services.wcm.extensions.publication.WCMPublicationServiceImpl - /Users/p___/pa___/pau___/paul/Private/Pictures/test/Cloud desk.jpg::intranet::paul
2012-03-07 18:14:34,110 [http-8080-42] WARN currentTenant=snatzo wcm:PublicationUpdateStateListener - Property not found publication:liveRevision
ThumbnailUtils:

2012-03-07 18:16:12,626 [http-8080-20] WARN currentTenant=snatzo wcm.WCMCoreUtils - Primary item not found for /Users/p___/pa___/pau___/paul/Private/Pictures/test
2012-03-07 18:16:12,641 [http-8080-20] WARN currentTenant=snatzo org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo - The content type: nt:folder isn't supported by any template
2012-03-07 18:16:14,349 [http-8080-42] WARN currentTenant=snatzo org.exoplatform.services.cms.thumbnail.impl.ThumbnailUtils - [collaboration] ADD NODE. Item already exists. Condition: parent ID, name, index. []:1[]Users:1[]p___:1[]pa___:1[]pau___:1[]paul:1[]Private:1[]Pictures:1[]test:1[http://www.exoplatform.com/jcr/exo/1.0]thumbnails:1, ID: ee602ad50a58ae7002ad96a93ee47e5e, ParentID: ee5dc5610a58ae700b49feb9a12a9f8b. Cause >>>> Duplicate entry 'collaboration-collaborationee5dc5610a58ae700b49feb9a12a9f8b-[htt' for key 'JCR_IDX_SITEM_PARENT': Duplicate entry 'collaboration-collaborationee5dc5610a58ae700b49feb9a12a9f8b-[htt' for key 'JCR_IDX_SITEM_PARENT'
2012-03-07 18:16:14,367 [http-8080-42] WARN currentTenant=snatzo org.exoplatform.services.cms.thumbnail.impl.ThumbnailUtils - [collaboration] ADD NODE. Item already exists. Condition: parent ID, name, index. []:1[]Users:1[]p___:1[]pa___:1[]pau___:1[]paul:1[]Private:1[]Pictures:1[]test:1[http://www.exoplatform.com/jcr/exo/1.0]thumbnails:1, ID: ee602ad50a58ae7002ad96a93ee47e5e, ParentID: ee5dc5610a58ae700b49feb9a12a9f8b. Cause >>>> Duplicate entry 'collaboration-collaborationee5dc5610a58ae700b49feb9a12a9f8b-[htt' for key 'JCR_IDX_SITEM_PARENT': Duplicate entry 'collaboration-collaborationee5dc5610a58ae700b49feb9a12a9f8b-[htt' for key 'JCR_IDX_SITEM_PARENT'
2012-03-07 18:16:14,569 [http-8080-42] WARN currentTenant=snatzo org.exoplatform.services.cms.thumbnail.impl.Thum
Goto http://localhost:8080/portal/g/:platform:web-contributors/siteExplorer and choose DMS Administration driver, then goto exo:ecm.
On console you'll get:

Apr 6, 2012 11:32:54 AM org.exoplatform.services.wcm.utils.WCMCoreUtils getMetadataTemplates
WARNING: Primary item not found for /exo:ecm
Apr 6, 2012 11:32:54 AM org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo getTemplate
WARNING: The content type: nt:unstructured isn't supported by any template
Fix description
Problem analysis
Unneccessary warning messages on ThumbnailUtils
Consistency warning when concurrent access to ThumbnailUtils, ThumbnailServiceImpl

How is the problem fixed?
 Remove Unneccessary warning messages on WCMCoreUtils
 Using synchronized for ThumbnailUtils.getThumbnailFolder(), ThumbnailUtils.getThumbnailNode()

Tests to perform
Reproduction test

Build, start, run platform on  cloud-workspaces.com and see logs
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

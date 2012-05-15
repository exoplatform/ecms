Summary
Update log of overall classes (1.2 profile)

CCP Issue:  N/A
Product Jira Issue: ECMS-3495.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
 Fix the violations problem on Sonar, especially related to LOG

Fix description
Problem analysis
Fix the violations problem on Sonar, especially related to LOG

How is the problem fixed?
- Declare log with sonar connention: private static final Log LOG  = ExoLogger.getLogger([class name].class.getName());
- Remove unnecessary WARNING:
    + in org.exoplatform.services.wcm.publication.WCMComposerImpl.getRemoteUser
    + org.exoplatform.services.wcm.utils.WCMCoreUtils.getMetadataTemplates
    + org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo.getTemplate

Patch file: PROD-ID.patch
Tests to perform
Reproduction test

Go to Sonar and see violations
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

Function or ClassName change: All java files using logging
Data (template, node type) migration/upgrade: 
Is there a performance risk/cost?
No


Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A

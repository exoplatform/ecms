Summary
Filter by web content on SCV portlet does not work

CCP Issue: N/A
Product Jira Issue: ECMS-3763.
Complexity: N/A
Impacted Client(s): N/A 
Proposal
 

Problem description
What is the problem to fix?

Login as John
Create new page with Content Detail portlet
Edit page
Select folder path to a folder containing web content
Select Filter by Web Contents at the top of Select Content form
> Result: Web contents are not displayed. These web contents are displayed when select filter "All"
Fix description
Problem analysis

In the template file, the value for filtering web contents is "Web Contents" (with "s"), but the value of this parameter in DriverConnector.java is "Web Content" (without "s"). So in this case, the DriverConnector will not correctly filter.
How is the problem fixed?

To fix this issue, we only change the value of filter type from "Web Content" to "Web Contents" in DriverConnector.java file.
 
Tests to perform
Reproduction test

Login as John
Create new page with Content Detail portlet
Edit page
Select folder path to a folder containing web content
Select Filter by Web Contents at the top of Select Content form. Web contents are not displayed. These web contents are displayed when select filter "All"
Tests performed at DevLevel

cf.above
Tests performed at Support Level

cf.above
Tests performed at QA

cf.above
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

N/A
Data (template, node type) migration/upgrade

No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A

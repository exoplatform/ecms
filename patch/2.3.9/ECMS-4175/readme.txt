Summary
	* Issue title: Overuse of idm connections in FileExplorer 
   	* CCP Issue:  N/A
    	* Product Jira Issue: ECMS-4175.
    	* Complexity: Normal

Proposal

 
Problem description

What is the problem to fix?
	* Overuse of idm connections in FileExplorer 

Fix description

Problem analysis
	* When getting Label for group drive, we use OrganizationService to get Group object, and then get label from Group object

How is the problem fixed?
	* To get Label for group drive, now we avoid using OrgranizationService. Instead of that, we get this from property exo:driveLabel
       of group drive node. This property is added/updated when the group is created/modified.

Tests to perform

Reproduction test
	* Performance test

Tests performed at DevLevel
	* cf aboves.

Tests performed at Support Level
	* Related functional tests

Tests performed at QA
	* Performance tests

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
	* No

Will previous configuration continue to work?

    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Any change in API (name, signature, annotation of a class/method)? No
   	* Data (template, node type) migration/upgrade: DriveDataUpgradePlugin

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	*

Support Comment
	* 

QA Feedbacks
	* TQA validated

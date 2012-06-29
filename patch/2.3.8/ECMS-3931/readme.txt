Summary
	* Issue title Exception during upgrading data from PLF 3.5.3 to PLF 3.5.4-SNAPSHOT
	* CCP Issue:  n/a
	* Product Jira Issue: ECMS-3931.
	*  Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?
	* Exception during upgrading data from PLF 3.5.3 to PLF 3.5.4-SNAPSHOT

Fix description

Problem analysis
	* If transaction is still running and the whole session is saved -> error.

How is the problem fixed?
	* Instead of saving whole session object then just saved the node which have modified to avoid transaction error problem during saving content.


Tests to perform

Reproduction test
	* Migrate from PLF 3.5.3 to PLF 3.5.4-SNAPSHOT with data test of TQA -> exception due to error in transaction
    	Dataset contains :
    		1/ 20.000 users
    		2/ 100 networks (each network has 200 members - each user has 199 connections)
    		3/ 200 spaces (10 spaces each has 10 members, 10 spaces x 50 members, 10 spaces x 100 members, 10 spaces x 250 members, 10 spaces x 500 members, 10 spaces x 1000 members, 160 spaces has no member)
    	4/ 4500 wiki pages
    	5/ 2100 topics

Tests performed at DevLevel
	* Start upgrade plugin to migrate data and see no exception in the console

Tests performed at Support Level
	* n/a

Tests performed at QA
	* Upgrade from PLF 3.5.3 to PLF 3.5.4 with TQA's dataset

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* No

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No change for documentation

Configuration changes

Configuration changes:
	* No change for configuration.

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change: UpgradeUserFolderPlugin
    Data (template, node type) migration/upgrade: Just change inside the upgrade plugin.

Is there a performance risk/cost?
	* No risk at all

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*


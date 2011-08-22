Summary

	* Status: PERF: Group Drive template
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2493.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix:
	* We only use a Group drive template to manage all Group drives. By doing that, we also remove the group listener that creates all the group drive at startup.

Fix description

How is the problem fixed?
	* Use only one Group drive template which is stored at JCR instead of all existing groups.
	* Group drives of each user are generated dynamically according to groups he belongs
	* Remove NewGroupListener

Patch information:

	Patch file: ECMS-2493.patch

Tests to perform

Reproduction test
	*

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*


Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* Remove New Group Listener in organization-component-plugins-configuration.xml
	* Add new Group configuration in dms-drives-configuration.xml

Will previous configuration continue to work?
	* No 

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Yes

Function or ClassName change*
	* dms-drives-configuration.xml, organization-component-plugins-configuration.xml

Is there a performance risk/cost?
	* Yes


Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*

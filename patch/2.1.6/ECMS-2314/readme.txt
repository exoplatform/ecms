Summary

	* Status: Exceptions are displayed because permissions were removed
	* CCP Issue: CCP-919, Product Jira Issue: ECMS-2314.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Exceptions are displayed because permissions were removed

Fix description

How is the problem fixed?
	* Update permission on the symlink node to similar with the target node

Patch information:
	* Patch file: ECMS-2314.patch

Tests to perform

Reproduction test
	1. Connect as root
	2. Go to Sites Explorer > Sites Management > acme >  web contents
	3. Create a new web content
	4. Click on "Manage categories"
	5. Go to the created document and click on "View permissions"
	6. Select the category "acme/Special Offers"
	7. Remove permissions for "any" and "*:/web-contributors"
	8. Logout and login as "mary"
	9. Go to Sites Explorer > acme > Special Offers -> Exception is raised

Tests performed at DevLevel
	* Do the same tasks like reproduction test => have no exception thrown

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change : No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*

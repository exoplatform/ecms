Summary
	* Status: PathNotFoundException after delete Favorites in Private Driver of user
	* CCP Issue: CCP-1056, Product Jira Issue: ECMS-2617.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* PathNotFoundException after deleting Favorites in Private Driver of user
	* Cannot delete a user's private Favorites folder after deleting another's private Favorites folder successully

Fix description

How is the problem fixed?
	* Add try catch PathNotFoundException block to avoid exception when Favorites does not exist
	* Use WCMCoreUtils.getSystemSessionProvider().getSession to get session of Trash
	* If Favorites does not exist, adding item to favorite will create new Favorites folder

Patch information:
	* Patch files: ECMS-2617.patch

Tests to perform

Reproduction test
	1. Login as root or john
	2. Go to Content Explorer
	3. Delete Favorite folder in the Private Driver -> Exception occurs
	4. UI error and Exception on console occur when another driver is chosen.

Tests performed at DevLevel
	*

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
	* Function or ClassName change: 
		core/services/src/main/java/org/exoplatform/services/cms/documents/impl/FavoriteServiceImpl.java
		core/services/src/main/java/org/exoplatform/services/cms/documents/impl/TrashServiceImpl.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

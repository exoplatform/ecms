Summary
	* Status: Problem in displaying category whose name contains special characters as "é" when browsing directories in CLV
	* CCP Issue: N/A, Product Jira Issue: ECMS-2764.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Problem with category whose name contains characters like the "é" when select browsing directories in CLV

Fix description

How is the problem fixed?
	* In Content Selector, node title is displayed in stead of node name when displaying category tree

Patch files: ECMS-2764.patch

Reproduction test
	* Go to Sites Explorer/acme
    	* Create category & sub category with names containing special characters as "é"
    	* Go to Site Editor/Add page wizard
    	* Input valid values then go to step 3
    	* Add Content detail portlet
    	* Click icon to edit content detail portlet
    	* Choose Select a content
    	* Browse to select content. Select the previously added directory => show category & sub category with names containing characters like the "é" not correct

Tests performed at DevLevel
	* cf above

Tests performed at QA/Support Level
	* cf above

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
	* N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*


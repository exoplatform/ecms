Summary
    	* Status: All search results and paginator aren't displayed when there are more than 1 pages of results
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2440.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* All search results and paginator aren't displayed when there are more than 1 pages of results

Fix description

How is the problem fixed?
	* The reason cause this error is the scroll bar for UIDocumentWorkspace component does not showed while displaying search result. To fix it overflow property of UIDocumentWorkspace should be changed from hidden to auto.


Patch files: ECMS-2440.patch

Reproduction test

Step to reproduce
    	* Sign in
    	* Go to Content Explorer > Sites Management > acme > web contents
    	* Import nodenews.xml
    	* Add category acme for each contents in imported folder.
    	* Go back Content Explorer > Sites Management > acme > categories > acme
    	* Search with keyword "news" -> Cannot see all results and cannot see paginator

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

Function or ClassName change
	* apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/UIListView.js

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*


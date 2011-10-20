Summary

	* Status: Name is not automatically generated anymore when creating a new content
	* CCP Issue: N/A, Product Jira Issue: ECMS-2441.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Name is not automatically generated anymore when creating a new content

Fix description

How is the problem fixed?
	* The reason causes this error is System does not load SELocalization javascript object in Components.js file.
	* To fix it: Update UIJCRExplorerPortlet.java file to ensure that Components.js file is loaded before rendering UIJCRExplorer portlet.

Patch information:
	* ECMS-2441.patch

Tests to perform

Reproduction test
	- When creating a new content (for example a new Article), after entering a title, the name is not generated any more.
	- The following JS error is shown : eXo.ecm.SELocalization is undefined

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
	* core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/UIJCRExplorerPortlet.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*

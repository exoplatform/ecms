Summary

	* Status: Invalid Item state when delete a node of a workspace different from trash one's
	* CCP Issue: CCP-1127, Product Jira Issue: ECMS-2826.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Invalid Item state when delete a node of a workspace different from trash one's

Fix description

How is the problem fixed?
	* Manipulate a node such as node.isNodeType("mix:referenceable"), node.getUUID() before deleting it from it's workspace instead of after deleting

Patch information:
	* Patch files: ECMS-2826.patch

Tests to perform

Reproduction test
	1. Go to *Content Explorer* > *DMS Administration*
	2. Upload a document then remove it > error on console and *Statuts Invalid* on browser

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
	* Function or ClassName change: core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/rightclick/manager/DeleteManageComponent.java
Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

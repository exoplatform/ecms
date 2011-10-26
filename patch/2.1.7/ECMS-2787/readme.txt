Summary
	* Status: Bug on search in document's comments
	* CCP Issue: CCP-1121, Product Jira Issue:  ECMS-2787.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Cannot search in document's comments

Fix description

How is the problem fixed?
	* Check if the result node is exo:contents node type. then get the sym-link node of 2 upper parent node
	* And Improve the getSymlinkNode() method: if the targetNode is null or is not the mix:referenceable then return null (that mean the target node haven't got any sym-link node)

Patch information:
	* Patch files: ECMS-2787.patch

Tests to perform

Reproduction test
	1. Go to the drive Events in the Sites Explorer
	2. Make a comment on any document on this drive
	3. Back to the root folder and make a search with any word which is found in the comment
		=> An error appears

Tests performed at DevLevel
	1. Go to the drive Events in the Sites Explorer
	2. Make a comment on any document on this drive
	3. Back to the root folder and do a search with any word which is found in the comment -> search result contains the node having the relevant comment

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* YES

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

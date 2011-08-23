Summary

	* Status: Exception when adding a select box multi valuated in edit meta data form
	* CCP Issue: [CCP-982@JIRA], *Product Jira Issue: [ECMS-2332@JIRA].
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Create a metadata form containing a multi valued select box.
	* Uploaded a content and edit its metadata:
		- Fill values, select some elements from the select box-> Save => Exception
	* A problem of cast is in execute method of UIAddMetadataForm@SaveActionListener when the selectbox is multivalued.

Fix description

How is the problem fixed?
	* Change the way to get values if the input UI is the SelectBox.

Patch information:
	Patch file: ECMS-2332.patch

Tests to perform

Reproduction test
	* See above

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
	* Function or ClassName change: no

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch validated

Support Comment
	* Patch validated

QA Feedbacks
	*

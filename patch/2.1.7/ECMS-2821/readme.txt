Summary
	* Status: Filter by tags doesn't work on Timeline View
	* CCP Issue: N/A, Product Jira Issue: ECMS-2821.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Filter by tags doesn't work on Timeline View

Fix description

How is the problem fixed?
	* In TimelineView, when displaying content, we haven't yet applied tags filter for it. To fix this issue, we only add tags filter for this view.

Patch information:
	* Patch files: ECMS-2821.patch

Tests to perform

Reproduction test
	* Go in the private drive
	* Upload 2 documents
	* Tag one document with "crash" for example
	* Tag the other document with "url"for example
	* Select tag cloud
	* Try to filter by "crash" or "url" tag, the 2 documents are displays.

Tests performed at DevLevel
	* cf. above

Tests performed at QA/Support Level
	* cf. above

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
	* core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/UIDocumentInfo.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

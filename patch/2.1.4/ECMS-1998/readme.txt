Summary

    * Status: Default taxonomy tree 's Child nodes are not displayed
    * CCP Issue: CCP-791, Product Jira Issue: ECMS-1998.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    *  Default taxonomy tree 's Child nodes are not displaye

Fix description

Problem analysis

    * When adding a taxonomy for a document, we list all taxonomy trees for the user to choose.
    * But in the initializing phase, we set the initial workspace as SystemWorkspace. It is true only the case that the first taxonomy tree in list is the System taxonomy tree.

How is the problem fixed?
    *  Set the initial workspace as the workspace of the first taxonomy tree in list.

Patch file: ECMS-1998.patch

Tests to perform

Reproduction test
* How to reproduce in PLF 3.0.3:
1)Login and navigate to /acme/documents in Site Management driver in file explorer.
2)Upload a document and add taxonomy to it.
Expected Result:The default taxonomy tree is intranet and we are able to see its child nodes.
Actual Result: The default taxonomy tree is intranet but there is no child node in this taxonomy tree with a "child not found" message
3)Choose to see another taxonomy.
4)Choose again intranet as taxonomy. Its child nodes are now displayed in the two panels as in this screenshot

The bug occurs when adding taxonomy after
    * either uploading a document
    * or creating a document

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
*No

Configuration changes

Configuration changes:
*No

Will previous configuration continue to work?
*Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: No

Is there a performance risk/cost?
*No

Validation (PM/Support/QA)

PM Comment
*PATCH VALIDATED BY PM

Support Comment
*Support review:patch validated

QA Feedbacks
*


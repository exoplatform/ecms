Summary

    * Status: Content duplicity check in "Multiple content selector" doesn't work properly
    * CCP Issue: CCP-853, Product Jira Issue: ECMS-1939.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

It is possible to select one content item more than once when
1) it is categorized in more categories
2) selecting the item once in a categorized drive ("acme", "classic" or "Events") and once in the "Managed sites" drive.
In general this applies to situations when an item has more than one "path" in JCR repository.

Fix description

How is the problem fixed?

    * Add a new information of each returned node: the target of symlink node (in the case of symlink) or the completed path, called it as targetPath.
    * When a new node selected, compare its own targetPath to every nodes which selected before, in the case of duplicated node found, raise the warning and reject selection of new node, otherwise, insert it. 

Patch files:ECMS-1939

Tests to perform

Reproduction test
* cf. above

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

    * Function or ClassName change: None

Is there a performance risk/cost?
* Yes, a new data is inserted that mean cost more memory for requesting.
Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


Summary

    * Status: When a link is created (multi filling) search returns the document in double
    * CCP Issue: N/A, Product Jira Issue: ECMS-2275.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
Steps to reproduce:

    * create a folder 1
    * create a folder 2
    * create a document in folder 1
    * multifile a document in folder 2
    * search a document in folder as parent 1

Fix description

How is the problem fixed?

    * Fixed BaseQueryTest#checkResult
    * Fixed Jcr2XcmisChangesListener#addEntry(String uuid, List<ContentEntry> addedEntries, Set<String> removedNodes)

Patch files:ECMS-2275.patch

Tests to perform

Reproduction test

Tests performed at DevLevel

    * Unit test: MultifilingUnfilingTest#testAddMultipleParents

Tests performed at QA/Support Level

    *  

Documentation changes

Documentation changes:

    *  no

Configuration changes

Configuration changes:

    * Will previous configuration continue to work?
    *  yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * no

Function or ClassName change

Is there a performance risk/cost?

    *  no

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


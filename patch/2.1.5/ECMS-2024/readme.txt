Summary

    * Status: Web content's long Comments are truncated in document view tab
    * CCP Issue: CCP-816, Product Jira Issue: ECMS-2024.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
1)add a free layout web content with a short main content.
2)add 3 long multi-line comments to this document.
3)click the show comments link in document view tab

Expected result : we are able to see the full comment by scrolling vertically
Actual result : the comment isn't fully displayed and there is no scroll bar
Fix description

How is the problem fixed?

    *  Add 2 properties into css package .Comments .CommentBox .CommentBoxCenterBG. It allow display scrollbar when comment of content is very long.

          max-height: 200px;

          overflow: auto;

    *  Create a javascript method (setScrollBar) and execute this method when user click "Show comments" link. It allow display scrollbar when have many comments for content.

    *  Updating the height of UIDocumentTabPanel element when user view document

Patch files:ECMS-2014.patch

Tests to perform

Reproduction test
1)add a free layout web content with a short main content.
2)add 3 long multi-line comments to this document.
3)click the show comments link in document view tab

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

    * N/A.

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


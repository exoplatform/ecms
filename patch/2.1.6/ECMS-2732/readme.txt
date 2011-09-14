Summary

    * Status: Replace automatically apostrophe characters in JS and CSS data by #39;
    * CCP Issue: N/A , Product Jira Issue: ECMS-2732.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Apostrophe characters in JS and CSS data are encoded to #39;

Fix description

How is the problem fixed?

    * Apostrophe character is kept as it is.

Patch file: ECMS-2732.patch

Tests to perform

Reproduction test
Case 1:

   1. Create new Free Web Content
   2. Fill CSS or JS text field with some lines containing apostrophe characters.
   3. Save as draft, we can see in these fields, apostrophe is automatically replaced by #39;

Case 2:
Create an RSS action for a node, then trigger it, we see the following exception in server console

***  RSS FEED BUILDING...   ***
Sep 14, 2011 10:05:06 AM org.exoplatform.services.cms.rss.impl.RSSServiceImpl generateRSS
SEVERE: Unexpected error
javax.jcr.query.InvalidQueryException: Unknown Query Construction : "#39;/sites". Lexical error at line 1, column 47.  Encountered: "#" (35), after : ""
at org.exoplatform.services.jcr.impl.core.query.sql.JCRSQLQueryBuilder.createQuery(JCRSQLQueryBuilder.java:172)
at org.exoplatform.services.jcr.impl.core.query.sql.QueryBuilder.createQueryTree(QueryBuilder.java:40)
...

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* N/A
Configuration changes

Configuration changes:
* N/A

Will previous configuration continue to work?
* Yes
Risks and impacts

Can this bug fix have any side effects on current client projects?
* This work around needs to be completed to avoid XSS attack.

Is there a performance risk/cost?
* N/A
Validation (PM/Support/QA)

PM Comment

    * Patch validated on behalf of PM.

Support Comment

    * Patch validated.

QA Feedbacks
*

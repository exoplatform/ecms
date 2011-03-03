Summary

    * Status: LocalizationConnector does not sanitize all special characters
    * CCP Issue: CCP-792, Product Jira Issue: ECMS-1999.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Impossible to open web contents in FO whose title contains special characters (with accents) in IE8.

Fix description

How is the problem fixed?

    * Add the mime-type for respond html page (Content-type: text/html; charset=utf-8)

Patch information:
Patch files: ECMS-1999.patch

Tests to perform

Reproduction test

   1. Log in as root at http://localhost:8080/ecmdemo (Using FF 3.6 or Google Chrome)
   2. In Edit mode, use the icon on the list viewer portlet to Add Content
   3. Create an article with title 'Rakı' (Note that the name will be set to rakı as a result of the LocalizationConnector sanitation)
   4. Go back to the home page and follow the link of the new article.
      This works in FF 3.6 or Chrome==>ok
      but fails with IE 8==>ko
      go to
      http://localhost:8080/ecmdemo/rest-ecmdemo/l11n/cleanName?name=Rakı --> RETURNS: rakı (special character ı - %C4%B1 NOT converted)

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
* Patch validated by PM

Support Comment
* Support review: Patch validated

QA Feedbacks
*
Labels parameters


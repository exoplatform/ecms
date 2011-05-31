Summary

    * Status: Rich Text Editor does not take into consideration CSS styles Chrome
    * CCP Issue: CCP-881, Product Jira Issue: ECMS-2137.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * In Chrome, Rich Text Editor does not take into consideration CSS styles.
      The desired behavior is :

   1. Rich Text Editor must take into consideration CSS styles as in Firefox
   2. Any change in CSS in advanced tab must be seen immediately in rich text editor when switching back to Main Content tab as in Firefox

Fix description

How is the problem fixed?

    * Changing check condition Browser to ensure that with Chrome browser, the CSS must be inserted to CKEditor.

Patch files:ECMS-2137.patch

Tests to perform

Reproduction test

   1. Go to Site Managements /acme/web contents/site artifacts and click on ACME Footer document, you should see a similar preview
      The label " Powered by eXo Platform 3.0" is well centered thanks to UIFooterPortlet
      Unknown macro: { text-align}

      style
   2. Edit this document, in rich text editor the label " Powered by eXo Platform 3.0" is not centered -> KO.
      However it is well displayed in Firefox.

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

    * N/A

Function or ClassName change

    * apps/resources-wcm/src/main/webapp/javascript/eXo/wcm/frontoffice/private/CKEditorUtil.js

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
*

Support Comment
* Patch validated

QA Feedbacks
*
Labels parameters


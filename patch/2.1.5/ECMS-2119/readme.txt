Summary

    * Status: In upload option of WCM content selector save button doesn't close popup Chrome only
    * CCP Issue: CCP-872, Product Jira Issue: ECMS-2119.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * In Chrome, when we use the upload option of WCM content selector, save button doesn't close popup but the uploaded image is saved

Fix description

How is the problem fixed?
This issue have two error:
1. "Invalid name" error with Chrome browser: In Content Selector, when user upload file and click "Save" button. System will get file name to update content. The problem is file name on Chrome browser includes file path (For example: when user upload file have abc.text in folder c:/document, then file name will be c:\document\abc.text with Chrome browser. With other browsers, File name is abc.text) -> "Invalid error". To fix it, we must ensuring that name file doesn't include path file.

2. Popup window doesn't closed when user click "Save" button: To fix it, we must ensuring that when user upload file successful then popup window must be closed by calling eXo.ecm.UploadForm.removeMask() medthod.

Patch files:ECMS-2119.patch

Tests to perform

Reproduction test
1. In Chrome, login by root
2. Select "Content Explorer", click "Add Document" tab to add new a document
3. At "Add Document" form, navigation to content field, click on "Select Content Link" button of CKEditor.
4. In "Content Selector" window, select acme drive, click on "Upload File" link
5. Choose file to upload, click on "Upload" button and then "Save" button -> Doesn't close popup but the uploaded image is saved

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

    * apps/resources-static/src/main/webapp/eXoPlugins/content/js/UploadForm.js

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


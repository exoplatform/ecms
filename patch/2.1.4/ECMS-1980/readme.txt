Summary

    * Status: Unable to open attachment in Content Detail portlet
    * CCP Issue: CCP-776, Product Jira Issue: ECMS-1980.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
In Content Explorer, when you create a new document using "Article" template and upload a PDF document as the attachment. The attachment can be opened in Content Explorer. However, it cannot be opened in the Content Detail Portlet. Below are the steps to replicate this issue:

1) Logon to ecmdemo application as root/gtn.
2) Go to "Group" -> "Content Explorer".
3) Navigate to "/acme/web contents/Events" folder.
4) Click "Add Document".
5) Select "Article" as the template, and enter some random information for other fields.
6) Click "Upload", and select a PDF document to upload as an attachment.
7) Click "Save as Draft".
8) Click "Manage Publications".
9) Click "Published" and then click "Save" button.
10) Click "Close" button.
11) Now click on the attachment link, you should be able to view the PDF document in Content Explorer (please refer to the attached "content-explorer-view-pdf.png" file).
12) Click "Site" -> "acme". You should be able to see the new content in the "Events" portlet.
13) Click "Read more" link for the new content.
14) You can see the attachment. However, the attachment can neither be downloaded nor opened (please refer to the attached "content-detail-view-pdf.png" file).
15) If you look at the page source, you will see the following:
<div class="AttachmentsContainer">
<div class="AttachmentsContentIcon">
<a onclick="config" style="cursor: pointer;">AIS-Channel-Portal-Solution-Report-v1.pdf</a>
</div>
It can be fixed by replacing

<a onclick="<%=uicomponent.event("ChangeNode", Utils.formatNodeName(att.getPath()), params)%>" style="cursor: pointer;">[<%=uicomponent.class%>]<%=att.getName()%></a>

by

<a href="<%=uicomponent.getDownloadLink(att)%>" style="cursor: pointer;"><%=att.getName()%></a>

Fix description

How is the problem fixed?

    * Change the parent node of the attachment item.
    * Change the template of event and sample node type to get the correct attachment link.

Patch information:
Patch files: ECMS-1980.patch

Tests to perform

Reproduction test
*

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

    * Function or ClassName change

Is there a performance risk/cost?
* NO

Validation (PM/Support/QA)

PM Comment
* Validated by PM

Support Comment
* Validated

QA Feedbacks
*


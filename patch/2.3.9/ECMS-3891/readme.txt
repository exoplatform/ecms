Impacted Client(s): N/A 
Summary
Issue title Checkbox never checked if it contains onchange=true
CCP Issue:  CCP-xyz 
Product Jira Issue: ECMS-3891.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

checkbox created by template cannot be checked
Fix description
Problem analysis

In UIDialogForm, we used object UIFormCheckBoxInput for the checkbox. So when the checkbox is updated, it makes a reload of the whole page
 

How is the problem fixed?

Remove object UIFormCheckBoxInput and use UICheckBoxInput instead
Tests to perform
Reproduction test

Go to Content administration->Content Types->Manage Node Type and upload this nodetype
Go to Content Presentation->Manage Templates and copy/paste this gtmpl code
<div class="UIForm FormLayout FormScrollLayout">
<% uiform.begin();
  /* start render action*/
  if (uiform.isShowActionsOnTop()) uiform.processRenderAction();
  /* end render action*/
%> 
<div class="HorizontalLayout">

<table class="UIFormGrid">

<tr>
<%
String[] fieldProperty; 
%>
<td class="FieldLabel"><%=_ctx.appRes("exo_testcheckbox_fg_n.dialog.label.name")%></td>
<td class="FieldComponent">
<%
String[] fieldName = ["jcrPath=/node", "editable=if-null", "validate=empty,name"];
uicomponent.addTextField("name", fieldName);
%>
</td>
</tr>
<tr>
<td class="FieldLabel"><%=_ctx.appRes("exo_testcheckbox_fg_n.dialog.label.exo_fg_p_checkboxfield")%></td>
<td class="FieldComponent">
<%
fieldProperty = ["jcrPath=/node/exo:fg_p_checkboxfield", "options=true,false", "onchange=true"];
uicomponent.addCheckBoxField("exo_fg_p_checkboxfield", fieldProperty);
%>
</td>
</tr>
</table>
</div>
<% /* start render action*/
  if (!uiform.isShowActionsOnTop()) uiform.processRenderAction();
  /* end render action*/
  uiform.end();
%>
</div>
Go to Site Explorer and add a new content using the created template
Try to check the checkbox.
Problem:  The checkbox is never checked.
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

N/A
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

No
Configuration changes
Configuration changes:

No
Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: No
Data (template, node type) migration/upgrade: No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: PR validated
Support Comment

Support review: PR validated
QA Feedbacks

N/A

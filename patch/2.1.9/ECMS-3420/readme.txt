Summary
webContent's javascript and CSS fields are escaped 
CCP Issue:  N/A
Product Jira Issue: ECMS-3420.
Complexity: N/A

Impacted Client(s): N/A 
Proposal
 

Problem description
What is the problem to fix?
The webContent's javascript field is escaped when saved, so the it is corrupted.
To reproduced :

create a new webContent in the Content Explorer
fill the title and the name
go to the Advanced tab
fill the javascript field with : alert("test");
save the webContent
close the webContent
Result: the webContent is displayed but the javascript is not executed (no alert). This is because the javascript is rendered as : alert("test");
We have the same issue if an html tag is inserted in the javascript, for example : alert('<div>'); is rendered as alert('<div>');</div>
The javascript field of webContent is not usable anymore.
This is also true for the CSS field.
Fix description
Problem analysis

In the content management sytem, its typical feature is enabling JavaScript in a content. This causes the XSS (Cross-site Scripting) attacks to the content displayed in the HTML format.However, there is no solution to keep JavaScript and to prevent the XSS attacks at the same time, so eXo Content allows you to decide whether JavaScript is allowed to run on a field of the content template or not by using the options parameter.
To allow JavaScript to execute, add "options = noSanitization" to the dialog template file. Normally, this file is named dialog1.gtmpl
By default, there is no "options = noSanitization" parameter in the dialog template file and this helps you prevent the XSS attacks. When end-users input JavaScript into a content, the JavaScript is automatically deleted when the content is saved.
How is the problem fixed?

Add "options = no Sanitization" into the dialog template of javascript and css files to avoid deleting and escaping content.

Tests to perform
Reproduction test

Create a new webContent in the Content Explorer
Fill the title and the name
Go to the Advanced tab
Fill the javascript field with : alert("test");
Save the webContent
Close the webContent, the webContent is displayed but the javascript is not executed (no alert). This is because the javascript is rendered as : alert("test"); We have the same issue if an html tag is inserted in the javascript, for example : alert('<div>'); is rendered as alert('<div>');</div>
The javascript field of webContent is not usable anymore. This is also true for the CSS field.
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

cf. above
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

N/A
Function or ClassName change

No
Data (template, node type) migration/upgrade

No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A

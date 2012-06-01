Summary
Should not display an activity when upload the image from the stream itself 

CCP Issue: N/A
Product Jira Issue: ECMS-3595.
Complexity: N/A
Summary
Proposal
Problem description
Fix description
Tests to perform
Changes in Test Referential
Documentation changes
Configuration changes
Risks and impacts
Validation (PM/Support/QA)
This page should represent the synthesis of information known about the issue fix.
This information will be used to create the general release notes file.
eXo internal information

Impacted Client(s): N/A 


Proposal
 

Problem description
What is the problem to fix?

When we upload an image from the stream itself then the activity should be disable on activity stream.
Fix description
Problem analysis

When a document is uploaded or created in Drive, a event will be broadcast to ecms-social integration handler. Therefore, an social activity will raise as a result. 
In order to share a document on activities stream, user has to do 2 steps such as:
Firstly, he upload to his drive -> Raising an ECMS activities 
Secondly, he select this document and press "Share" -> Raising a Social activities
-> That's why user always see 2 activities and the first one is redundant, it need to be skipped.
How is the problem fixed?

In Every time, if user creates new document in a Drive -> That's a normal behavior but cause a inconvenience situation for user -> It should be fixed
Solution:
Create a current context for a specific document , which stores some auxiliary attributes of a document and useful for document listeners which are able to make decision based on these attributes.
This context looks like:
public class DocumentContext {
private static ThreadLocal<DocumentContext> current = new ThreadLocal<DocumentContext>();

public static DocumentContext getCurrent() {
   if (current.get() == null) {
      setCurrent(new DocumentContext());
    }
   return current.get();
  }

....
}
Each time, attributes are able to set and get via

/**
   * @return the attributes
   */
  public HashMap<String, Object> getAttributes() {
   return attributes;
  }

  /**
   * @param attributes the attributes to set
   */
  public void setAttributes(HashMap<String, Object> attributes) {
    this.attributes = attributes;
  }
}
To skip raising activities when user using ManageDocumentService to upload file, we use

DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, true);
Then this activities is indicated to skip at ecms-social activities handler:

 Object isSkipRaiseAct = DocumentContext.getCurrent().getAttributes().get(DocumentContext.IS_SKIP_RAISE_ACT);
if (isSkipRaiseAct != null && Boolean.valueOf(isSkipRaiseAct.toString())) {
  return;
 }
Patch file: PROD-ID.patch
Tests to perform
Reproduction test

cf.above
Tests performed at DevLevel

cf.above
Tests performed at Support Level

cf.above
Tests performed at QA

cf.above
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

Update Ref guide here DOC-1606
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
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A

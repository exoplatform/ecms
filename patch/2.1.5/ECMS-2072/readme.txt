Summary

    * Status: ContentExplorer: Main content of webcontent item is deleted on "Save draft" if user invokes Image Properties dialog in CKEditor
    * CCP Issue: CCP-844, Product Jira Issue: ECMS-2072.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    *  ContentExplorer: Main content of webcontent item is deleted on "Save draft" if user invokes Image Properties dialog in CKEditor

Fix description

How is the problem fixed?

    * Changing commit function of "Image Properties" component in CKEditor. In commit function
         1. Before: If type of commit equals "CLEANUP" then remove "src" of CKEditor
            ?
            commit : function( type, element )
            {
                if ( type == IMAGE && ( this.getValue() || this.isChanged() ) )
                {
                    element.data( 'cke-saved-src', this.getValue() );
                    element.setAttribute( 'src', this.getValue() );
                }
                else if ( type == CLEANUP )
                {
                    element.setAttribute( 'src', '' );  // If removeAttribute doesn't work.
                    element.removeAttribute( 'src' );
                }
            }
         2. After: Remove block code correspond with case above.
            ?
            commit : function( type, element )
            {
                if ( type == IMAGE && ( this.getValue() || this.isChanged() ) )
                {
                    element.data( 'cke-saved-src', this.getValue() );
                    element.setAttribute( 'src', this.getValue() );
                }  
            }

Patch files:ECMS-2072

Tests to perform

Reproduction test
* Steps to reproduce:
1) go to the ACME site and switch the portal to edit mode
2) edit one of the items from Company News CLV portlet (via on-mouse-over edit icon)
3) insert an image to main content via WCM Content Selector in CKEditor
4) invoke Image Properties dialog (through popup menu or by doubleclick on the image) and close it
5) save draft

The content is completely blank.

The content is saved fine if the step 4 is ommited. The problem doesn't occur when editing through Group-->ContentExplorer.

Tests performed at DevLevel

    * cf. above

Tests performed at QA/Support Level

    * cf. above

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

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


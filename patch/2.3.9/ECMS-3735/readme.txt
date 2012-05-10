Summary: Back button in file explorer does not keep the current "page" when we have pagination 

 * CCP Issue: N/A
 * Product Jira Issue: ECMS-3735.
 ** Complexity: N/A
 

Problem description
What is the problem to fix?
  * When you have many files in a folder, you have pagination in the file explorer window. (and the tree view), but when I go back from a "view" of a file, the current page is not kept.
  * The user always go back to page 1.
    
Fix description

Problem analysis
  * There is not a mechanism storing page index in history while surfing the nodes and node has paninator. 
  * Then, when click Back button, first page allways be navigated to

How is the problem fixed?
  * Record last pageIndex of paginator of node to history
  * When clicking backbutton, go to last page index of paginator of node
  * Page index still remains when click Copy, Cut on context menu - actions do not make any modifications on node

Tests to perform
Reproduction test
    1- put many file in a folder (to have for example 3 pages)
    2- go in page 3
    3- select one file on page 3
    4- click on the back button of the file explorer
    5- BUG: the current page and list of file is page 1

Tests performed at DevLevel

* Cf above
Tests performed at Support Level

* Cf above
Tests performed at QA

* Cf above

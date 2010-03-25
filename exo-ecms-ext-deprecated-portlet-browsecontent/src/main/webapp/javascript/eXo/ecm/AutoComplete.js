function AutoComplete() {
	var Self = this;

	//set private property;
	var DOM = eXo.core.DOMUtil;


	//--------------------------------------------------------------------------------
	AutoComplete.prototype.createTags = function(event, clickedElement) {
		if (eXo.ecm.AutoComplete.count) {
			return false;
		}
		eXo.ecm.AutoComplete.count = 1;
		// Basic UA detection

		isIE = document.all ? true : false;
		isGecko = navigator.userAgent.toLowerCase().indexOf('gecko') != -1;
		isOpera = navigator.userAgent.toLowerCase().indexOf('opera') != -1;

		var event = event || window.event;
		event.cancelBubble = true; 
		popupSelector = DOM.findAncestorByClass(clickedElement, "UITaggingForm");
		showBlock = DOM.findFirstDescendantByClass(popupSelector,"div", "UIFormTextAreaInput");
		itemList = DOM.findDescendantsByClass(showBlock, "option", "Item");
		tagNameInput = DOM.findFirstDescendantByClass(popupSelector,"div", "UITagNameInput");
		inputBox = DOM.findDescendantById(tagNameInput, "names");
		tags = new Array(itemList.length);
		for (var i = 0; i < itemList.length; i++) {
			var item = itemList[i];
			tags[i] = item.getAttributeNode("value").value;
		}
		eXo.ecm.AutoComplete.Tags = {'data':tags,
				'isVisible':false,
				'element': inputBox,
				'dropdown':null,
				'highlighted':null};

		eXo.ecm.AutoComplete.Tags['element'].setAttribute('autocomplete', 'off');
		eXo.ecm.AutoComplete.Tags['element'].onkeydown  = function(e) {return AutoComplete.prototype.keyDown(e);}
		eXo.ecm.AutoComplete.Tags['element'].onkeyup    = function(e) {return AutoComplete.prototype.keyUp(e);}
		eXo.ecm.AutoComplete.Tags['element'].onkeypress = function(e) {
			if (!e) e = window.event;
			if (e.keyCode ==13 || isOpera) return false;
		}
		eXo.ecm.AutoComplete.Tags['element'].ondblclick = function()  {AutoComplete.prototype.showDropdown();}
		eXo.ecm.AutoComplete.Tags['element'].onclick    = function(e) {if (!e) e = window.event; e.cancelBubble = true; e.returnValue = false;}

		// Hides the dropdowns when document clicked
		var docClick = function()
		{
			AutoComplete.prototype.hideDropdown();
		}

		if (document.addEventListener) {
			document.addEventListener('click', docClick, false);
		} else if (document.attachEvent) {
			document.attachEvent('onclick', docClick, false);
		}

		// Max number of items shown at once
		if (arguments[2] != null) {
			eXo.ecm.AutoComplete.Tags['maxitems'] = arguments[2];
			eXo.ecm.AutoComplete.Tags['firstItemShowing'] = 0;
			eXo.ecm.AutoComplete.Tags['lastItemShowing']  = arguments[2] - 1;
		}

		AutoComplete.prototype.createDropdown();

		// Prevent select dropdowns showing thru
		if (isIE) {
			eXo.ecm.AutoComplete.Tags['iframe'] = document.createElement('iframe');
			eXo.ecm.AutoComplete.Tags['iframe'].id = 'names' +'_iframe';
			eXo.ecm.AutoComplete.Tags['iframe'].style.position = 'absolute';
			eXo.ecm.AutoComplete.Tags['iframe'].style.top = '75';
			eXo.ecm.AutoComplete.Tags['iframe'].style.left = '148';
			eXo.ecm.AutoComplete.Tags['iframe'].style.width = '0px';
			eXo.ecm.AutoComplete.Tags['iframe'].style.height = '0px';
			eXo.ecm.AutoComplete.Tags['iframe'].style.zIndex = '98';
			eXo.ecm.AutoComplete.Tags['iframe'].style.visibility = 'hidden';

			eXo.ecm.AutoComplete.Tags['element'].parentNode.insertBefore(eXo.ecm.AutoComplete.Tags['iframe'], eXo.ecm.AutoComplete.Tags['element']);
		}
	}

	/**
	 * Creates the dropdown layer
	 */
	AutoComplete.prototype.createDropdown = function()
	{
		isIE = document.all ? true : false;
		var left = 0;
		var top = 0;
		if (isIE) {
				left = 148;
				top = 75;
		}
		else {
				left  = AutoComplete.prototype.getLeft(eXo.ecm.AutoComplete.Tags['element']);
				top   = AutoComplete.prototype.getTop(eXo.ecm.AutoComplete.Tags['element']) + eXo.ecm.AutoComplete.Tags['element'].offsetHeight;
		}
		var width = eXo.ecm.AutoComplete.Tags['element'].offsetWidth;

		eXo.ecm.AutoComplete.Tags['dropdown'] = document.createElement('div');
		eXo.ecm.AutoComplete.Tags['dropdown'].className = 'autocomplete'; // Don't use setAttribute()

		eXo.ecm.AutoComplete.Tags['element'].parentNode.insertBefore(eXo.ecm.AutoComplete.Tags['dropdown'], eXo.ecm.AutoComplete.Tags['element']);

		// Position it
		eXo.ecm.AutoComplete.Tags['dropdown'].style.left       = left + 'px';
		eXo.ecm.AutoComplete.Tags['dropdown'].style.top        = top + 'px';
		eXo.ecm.AutoComplete.Tags['dropdown'].style.width      = width + 'px';
		eXo.ecm.AutoComplete.Tags['dropdown'].style.zIndex     = '99';
		eXo.ecm.AutoComplete.Tags['dropdown'].style.visibility = 'hidden';
	}

	/**
	 * Gets left coord of given element
	 * 
	 * @param object element The element to get the left coord for
	 */
	AutoComplete.prototype.getLeft = function(element)
	{
		var curNode = element;
		var left    = 0;
		do {
			left += curNode.offsetLeft;
			curNode = curNode.offsetParent;

		} while(curNode.tagName.toLowerCase() != 'div');
		return left;
	}

	/**
	 * Gets top coord of given element
	 * 
	 * @param object element The element to get the top coord for
	 */
	AutoComplete.prototype.getTop = function(element)
	{
		var curNode = element;
		var top    = 0;
		do {
			top += curNode.offsetTop;
			curNode = curNode.offsetParent;
		} while(curNode.tagName.toLowerCase() != 'div');
		return top;
	}   

	/**
	 * Shows the dropdown layer
	 * 
	 */
	AutoComplete.prototype.showDropdown = function()
	{
		AutoComplete.prototype.hideAll();

		var value = eXo.ecm.AutoComplete.Tags['element'].value;
		var toDisplay = new Array();
		var newDiv    = null;
		var text      = null;
		var numItems  = eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length;

		// Remove all child nodes from dropdown
		while (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length > 0) {
			eXo.ecm.AutoComplete.Tags['dropdown'].removeChild(eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[0]);
		}

		// Go thru data searching for matches
		for (i=0; i<eXo.ecm.AutoComplete.Tags['data'].length; ++i) {
			if (eXo.ecm.AutoComplete.Tags['data'][i].substr(0, value.length) == value) {
				toDisplay[toDisplay.length] = eXo.ecm.AutoComplete.Tags['data'][i];
			}
		}

		// No matches?
		if (toDisplay.length == 0) {
			AutoComplete.prototype.hideDropdown();
			return;
		}

		// Add data to the dropdown layer
		for (i=0; i<toDisplay.length; ++i) {
			newDiv = document.createElement('div');
			newDiv.className = 'autocomplete_item'; // Don't use setAttribute()
			newDiv.setAttribute('id', 'autocomplete_item_' + i);
			newDiv.setAttribute('index', i);
			newDiv.style.zIndex = '99';

			// Scrollbars are on display ?
			if (toDisplay.length > eXo.ecm.AutoComplete.Tags['maxitems'] && navigator.userAgent.indexOf('MSIE') == -1) {
				newDiv.style.width = eXo.ecm.AutoComplete.Tags['element'].offsetWidth - 22 + 'px';
			}

			newDiv.onmouseover = function() {AutoComplete.prototype.highlightItem(this.getAttribute('index'));};
			newDiv.onclick     = function() {AutoComplete.prototype.setValue(); AutoComplete.prototype.hideDropdown();AutoComplete.prototype.disposeCount();}

			text   = document.createTextNode(toDisplay[i]);
			newDiv.appendChild(text);

			eXo.ecm.AutoComplete.Tags['dropdown'].appendChild(newDiv);
		}


		// Too many items?
		if (toDisplay.length > eXo.ecm.AutoComplete.Tags['maxitems']) {
			eXo.ecm.AutoComplete.Tags['dropdown'].style.height = (eXo.ecm.AutoComplete.Tags['maxitems'] * 15) + 2 + 'px';

		} else {
			eXo.ecm.AutoComplete.Tags['dropdown'].style.height = '';
		}


		/**
		 * Set left/top in case of document movement/scroll/window resize etc
		 */
		var left = (isIE ? 30 : 0) + AutoComplete.prototype.getLeft(eXo.ecm.AutoComplete.Tags['element']); 
		var top = (isIE ? 50 : 0) + AutoComplete.prototype.getTop(eXo.ecm.AutoComplete.Tags['element']) + eXo.ecm.AutoComplete.Tags['element'].offsetHeight;
		
		eXo.ecm.AutoComplete.Tags['dropdown'].style.left = left;
		eXo.ecm.AutoComplete.Tags['dropdown'].style.top  = top;

		// Show the iframe for IE
		if (isIE) {
			eXo.ecm.AutoComplete.Tags['iframe'].style.top    = eXo.ecm.AutoComplete.Tags['dropdown'].style.top;
			eXo.ecm.AutoComplete.Tags['iframe'].style.left   = eXo.ecm.AutoComplete.Tags['dropdown'].style.left;
			eXo.ecm.AutoComplete.Tags['iframe'].style.width  = eXo.ecm.AutoComplete.Tags['dropdown'].offsetWidth;
			eXo.ecm.AutoComplete.Tags['iframe'].style.height = eXo.ecm.AutoComplete.Tags['dropdown'].offsetHeight;

			eXo.ecm.AutoComplete.Tags['iframe'].style.visibility = 'visible';
		}


		// Show dropdown
		if (!eXo.ecm.AutoComplete.Tags['isVisible']) {
			eXo.ecm.AutoComplete.Tags['dropdown'].style.visibility = 'visible';
			eXo.ecm.AutoComplete.Tags['isVisible'] = true;
		}


		// If now showing less items than before, reset the highlighted value
		if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length != numItems) {
			eXo.ecm.AutoComplete.Tags['highlighted'] = null;
		}
	}    

	/**
	 * Hides the dropdown layer
	 * 
	 */
	AutoComplete.prototype.hideDropdown = function()
	{
		if (eXo.ecm.AutoComplete.Tags['iframe']) {
			eXo.ecm.AutoComplete.Tags['iframe'].style.visibility = 'hidden';
		}

		eXo.ecm.AutoComplete.Tags['dropdown'].style.visibility = 'hidden';
		eXo.ecm.AutoComplete.Tags['highlighted'] = null;
		eXo.ecm.AutoComplete.Tags['isVisible']   = false;
	}

	/**
	 * Hides all dropdowns
	 */
	AutoComplete.prototype.hideAll = function()
	{
		AutoComplete.prototype.hideDropdown();
	}    

	/**
	 * Highlights a specific item
	 * 
	 * @param int    index The index of the element in the dropdown to highlight
	 */
	AutoComplete.prototype.highlightItem = function(index)
	{
		if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[index]) {
			for (var i=0; i<eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length; ++i) {
				if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[i].className == 'autocomplete_item_highlighted') {
					eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[i].className = 'autocomplete_item';
				}
			}

			eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[index].className = 'autocomplete_item_highlighted';
			eXo.ecm.AutoComplete.Tags['highlighted'] = index;
		}
	}   

	/**
	 * Highlights the menu item with the given index
	 * 
	 * @param int    index The index of the element in the dropdown to highlight
	 */
	AutoComplete.prototype.highlight = function(index)
	{
		// Out of bounds checking
		if (index == 1 && eXo.ecm.AutoComplete.Tags['highlighted'] == eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length - 1) {
			eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[eXo.ecm.AutoComplete.Tags['highlighted']].className = 'autocomplete_item';
			eXo.ecm.AutoComplete.Tags['highlighted'] = null;

		} else if (index == -1 && eXo.ecm.AutoComplete.Tags['highlighted'] == 0) {
			eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[0].className = 'autocomplete_item';
			eXo.ecm.AutoComplete.Tags['highlighted'] = eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length;
		}

		// Nothing highlighted at the moment
		if (eXo.ecm.AutoComplete.Tags['highlighted'] == null) {
			if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes.length > 0) {
				eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[0].className = 'autocomplete_item_highlighted';
				eXo.ecm.AutoComplete.Tags['highlighted'] = 0;
			}
		} else {
			if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[eXo.ecm.AutoComplete.Tags['highlighted']]) {
				eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[eXo.ecm.AutoComplete.Tags['highlighted']].className = 'autocomplete_item';
			}

			var newIndex = eXo.ecm.AutoComplete.Tags['highlighted'] + index;

			if (eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[newIndex]) {
				eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[newIndex].className = 'autocomplete_item_highlighted';

				eXo.ecm.AutoComplete.Tags['highlighted'] = newIndex;
			}
		}
	}


	/**
	 * Sets the input to a given value
	 * 
	 */
	AutoComplete.prototype.setValue = function()
	{
		eXo.ecm.AutoComplete.Tags['element'].value = eXo.ecm.AutoComplete.Tags['dropdown'].childNodes[eXo.ecm.AutoComplete.Tags['highlighted']].innerHTML;
	}

	/**
	 * Checks if the dropdown needs scrolling
	 * 
	 */
	AutoComplete.prototype.scrollCheck = function()
	{
		// Scroll down, or wrapping around from scroll up
		if (eXo.ecm.AutoComplete.Tags['highlighted'] > eXo.ecm.AutoComplete.Tags['lastItemShowing']) {
			eXo.ecm.AutoComplete.Tags['firstItemShowing'] = eXo.ecm.AutoComplete.Tags['highlighted'] - (eXo.ecm.AutoComplete.Tags['maxitems'] - 1);
			eXo.ecm.AutoComplete.Tags['lastItemShowing']  = eXo.ecm.AutoComplete.Tags['highlighted'];
		}

		// Scroll up, or wrapping around from scroll down
		if (eXo.ecm.AutoComplete.Tags['highlighted'] < eXo.ecm.AutoComplete.Tags['firstItemShowing']) {
			eXo.ecm.AutoComplete.Tags['firstItemShowing'] = eXo.ecm.AutoComplete.Tags['highlighted'];
			eXo.ecm.AutoComplete.Tags['lastItemShowing']  = eXo.ecm.AutoComplete.Tags['highlighted'] + (eXo.ecm.AutoComplete.Tags['maxitems'] - 1);
		}

		eXo.ecm.AutoComplete.Tags['dropdown'].scrollTop = eXo.ecm.AutoComplete.Tags['firstItemShowing'] * 15;
	}

	/**
	 * Function which handles the keypress event
	 * 
	 */
	AutoComplete.prototype.keyDown = function(event)
	{
		// Mozilla
		if (arguments[1] != null) {
			event = arguments[1];
		}
		if (!event) event = window.event;
		var keyCode = event.keyCode || event.which;
		switch (keyCode) {

		// Return/Enter
		case 13:
			if (eXo.ecm.AutoComplete.Tags['highlighted'] != null) {
				AutoComplete.prototype.setValue();
				AutoComplete.prototype.hideDropdown();
			}

			event.returnValue = false;
			event.cancelBubble = true;
			break;

			// Escape
		case 27:
			AutoComplete.prototype.hideDropdown();
			event.returnValue = false;
			event.cancelBubble = true;
			break;

			// Up arrow
		case 38:
			if (!eXo.ecm.AutoComplete.Tags['isVisible']) {
				AutoComplete.prototype.showDropdown();
			}

			AutoComplete.prototype.highlight( -1);
			AutoComplete.prototype.scrollCheck( -1);
			return false;
			break;

			// Tab
		case 9:
			if (eXo.ecm.AutoComplete.Tags['isVisible']) {
				AutoComplete.prototype.hideDropdown();
			}
			return;

			// Down arrow
		case 40:
			if (!eXo.ecm.AutoComplete.Tags['isVisible']) {
				AutoComplete.prototype.showDropdown();
			}

			AutoComplete.prototype.highlight(1);
			AutoComplete.prototype.scrollCheck(1);
			return false;
			break;
		}
	}  

	/**
	 * Function which handles the keyup event
	 * 
	 */
	AutoComplete.prototype.keyUp = function(event)
	{
		// Mozilla
		if (arguments[1] != null) {
			event = arguments[1];
		}
		if (!event) event = window.event;
		var keyCode = event.keyCode || event.which;
		switch (keyCode) {
		case 13:
			event.returnValue = false;
			event.cancelBubble = true;
			break;

		case 27:
			AutoComplete.prototype.hideDropdown();
			event.returnValue = false;
			event.cancelBubble = true;
			break;

		case 38:
		case 40:
			return false;
			break;

		default:
			AutoComplete.prototype.showDropdown();
		break;
		}
	}

	/**
	 * Returns whether the dropdown is visible
	 * 
	 */
	AutoComplete.prototype.isVisible = function()
	{
		return eXo.ecm.AutoComplete.Tags['dropdown'].style.visibility == 'visible';
	}
	
	AutoComplete.prototype.disposeCount = function() {
		delete eXo.ecm.AutoComplete.count;
	}

};

eXo.ecm.AutoComplete = new AutoComplete();

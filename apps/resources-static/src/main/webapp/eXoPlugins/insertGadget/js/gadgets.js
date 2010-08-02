var CKEDITOR		= window.opener.CKEDITOR || {};
var editor			= CKEDITOR.insertGadget ;
var oFakeImage = editor.Selection.GetSelectedElement() ;
eval('var metadata = ' + oFakeImage.getAttribute('_fckgadgetmetadata').replace(/\${quote}/g, '"') + '.gadgets[0]');
eval('var userPrefs = ' + oFakeImage.getAttribute('_fckgadgetuserprefs').replace(/\${quote}/g, '"'));
var gadgetId = oFakeImage.getAttribute('_fckgadgetnumber');
var gadgetUrl = oFakeImage.getAttribute('_fckgadgeturl');
var prefs = metadata.userPrefs;
var tmp = '';
for (var i in prefs) {
	var type = prefs[i].type;
	if (type == "list" || type == "hidden") tmp += 'x';
	else tmp += 'o';
}
window.parent.AddTab( 'Preview', 'Preview' ) ;
if (tmp.indexOf('o') >= 0) window.parent.AddTab( 'UserPreference', 'User Preference' ) ;

function OnDialogTabChange( tabCode ) {
	ShowE('UserPreference', ( tabCode == 'UserPreference' ) ) ;
	ShowE('Preview', ( tabCode == 'Preview' ) ) ;
	top.eXo.gadget.UIGadget
	if (tabCode == 'UserPreference') {
		generateForm(metadata, userPrefs);	
	}
}

window.onload = function() {
	ShowGadget(gadgetUrl);
	window.parent.SetOkButton( true );
}

function Ok() {
	var srcfull = FCK.GetData();
	var aScripts = srcfull.split(/(<script type="text\/javascript">.*?<\/script>)/);
	var iMark = 0;
	var sMark = '';
	for (var i in aScripts) {
		var sScript = aScripts[i];
		if (typeof sScript == 'string' && sScript.indexOf('WCM gadgets random"' + gadgetId + '"') >= 0) {
			iMark = i;
			sMark = "<scri" + "pt type=\"text/javascript\">/*WCM gadgets random\"" + gadgetId + "\" metadata\"" + oFakeImage.getAttribute('_fckgadgetmetadata') + "\" userprefs\"" + top.eXo.core.JSON.stringify(getUserPrefs()).replace(/"/g, '${quote}') + "\" thumbnail\"" + oFakeImage.getAttribute('_fckgadgetthumbnail') + "\" url\"" + gadgetUrl + "\"*/eXo.core.Browser.addOnLoadCallback('" + gadgetId + "', function() {eXo.gadget.UIGadget.createGadget('" + gadgetUrl + "','" + gadgetId + "', " + oFakeImage.getAttribute('_fckgadgetmetadata').replace(/\${quote}/g, '"') + ", " + top.eXo.core.JSON.stringify(getUserPrefs()) + ", 'home', 1, 0, 0)});</scri" + "pt>";
		}
	}
	aScripts[iMark] = sMark;
	FCK.SetData(aScripts.join(''));
	return true;
}

function generateForm(metadata, userPrefs) {
	var prefs = metadata.userPrefs;
	var parentEl = GetE('UserPreference');
	var formEl = document.createElement("form");
	var tableEl = document.createElement("table");
	tableEl.className = 'UIFormGrid';
    var prefix = "m_" + gadgetId + "_up_";
	
	var j = 0;
	for (var att in prefs) {
		var type = prefs[att].type;
		if(type == "list"|| type == "hidden") continue;
		
		var trEl = tableEl.insertRow(tableEl.rows.length);
        var labelEl = trEl.insertCell(0);
		labelEl.className = 'FieldLabel';
		var attEl = trEl.insertCell(1);
		attEl.className = 'FieldComponent';

        var elID = "m_" + gadgetId + '_' + j;

        labelEl.innerHTML = prefs[att].displayName + ": ";
        if (type == "enum") {
            var el = document.createElement("select");
            el.name = prefix + att;
            var values = prefs[att].orderedEnumValues;
            
			var userValue = userprefs[att];

            for (var i = 0; i < values.length; i++) {
                var value = values[i];
                var optEl = document.createElement("option");
                theText = document.createTextNode(value.displayValue);
                optEl.appendChild(theText);
                optEl.setAttribute("value", value.value);
                if(userValue && value.value == userValue)
                    optEl.setAttribute("selected", "selected");  
                el.appendChild(optEl);
            }
            el.id = elID;
            attEl.appendChild(el);
        } else if (type == "string" || type == "number") {
            var el = document.createElement("input");
            el.name = prefix + att;
            el.id = elID;
            
			if (userPrefs[att]) {
                el.value = userPrefs[att];
            }
			
            attEl.appendChild(el);
        }
        formEl.appendChild(tableEl);
        j++;
    }
	
	var numFieldsEl = document.createElement("input");
    numFieldsEl.type = "hidden";
    numFieldsEl.value = j;
    numFieldsEl.id = "m_" + gadgetId + "_numfields";
    formEl.appendChild(numFieldsEl);
	
	parentEl.appendChild(formEl);
}

function ShowGadget(url) {
	GetE('Preview').innerHTML = getMainContent();
}

//private function
var GADGET = new Object();
GADGET.gadgetIframePrefix = 'remote_iframe_';
GADGET.cssClassGadgetContent = 'gadgets-gadget-content';
GADGET.cssClassGadget = 'gadgets-gadget';

GADGET.serverBase_ = '/eXoGadgetServer/gadgets/';
GADGET.container = 'default';
GADGET.nocache = 1;
GADGET.country = 'ALL';
GADGET.language = 'ALL';
GADGET.view = 'default';
GADGET.parentUrl = 'http://' + document.location.host;
GADGET.secureToken = 'root:john:appid:cont:url:0';
GADGET.rpcToken = (0x7FFFFFFF * Math.random()) | 0; 

function getIframeUrl() {
  return GADGET.serverBase_ + 'ifr?' +
      'container=' + GADGET.container +
      '&mid=0'+
      '&nocache=' + GADGET.nocache +
      '&country=' + GADGET.country +
      '&lang=' + GADGET.language +
      '&view=' + GADGET.view +
      '&v=' +  //(this.specVersion ? '&v=' + this.specVersion : '') +
      '&parent=' + encodeURIComponent(GADGET.parentUrl) +
      //this.getAdditionalParams() +
      getUserPrefsParams() +
      '&st=' + encodeURIComponent(GADGET.secureToken) +
      '&url=' + encodeURIComponent(gadgetUrl) +
      '#rpctoken=' + GADGET.rpcToken;
      //(this.viewParams ? '&view-params=' +  encodeURIComponent(JSON.stringify(this.viewParams)) : '') +
      //(this.hashData ? '&' + this.hashData : '');
};

function getMainContent() {
	var iframeId = GADGET.gadgetIframePrefix + gadgetId;
	return '<div class="' + GADGET.cssClassGadgetContent + '"><iframe id="' +
      iframeId + '" name="' + iframeId + '" class="' + GADGET.cssClassGadget +
      '" src="' + getIframeUrl() +
      '" frameborder="no" scrolling="yes"' +
      ' height="100%"' + // (this.height ? ' height="' + this.height + '"' : '') +
	  'width="100%"' + //(this.width ? ' width="' + this.width + '"' : 'width="100%"') +
      '></iframe></div>';
}

function getUserPrefs() {
	var prefs = {};
	if (!document.getElementById('m_' + gadgetId + '_numfields')) return prefs;
	var numFields = document.getElementById('m_' + gadgetId + '_numfields').value;
	for (var i = 0; i < numFields; i++) {
		var input = document.getElementById('m_' + gadgetId + '_' + i);
		if (input.type != 'hidden') {
		  var userPrefNamePrefix = 'm_' + gadgetId + '_up_';
		  var userPrefName = input.name.substring(userPrefNamePrefix.length);
		  var userPrefValue = input.value;
		  prefs[userPrefName] = userPrefValue;
		}
	}
	return prefs;
}

function getUserPrefsParams() {	
  var params = '';
  var prefs = getUserPrefs();
  if (prefs) {
    for(var name in prefs) {
		params += '&up_' + encodeURIComponent(name) + '=' + encodeURIComponent(prefs[name]);
    }
  }
  return params;
}

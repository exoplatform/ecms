function UIFormGeneratorPortlet() {
}

UIFormGeneratorPortlet.prototype.init = function() {
	var DOMUtil = eXo.core.DOMUtil;
	var uiTabContentContainer = document.getElementById('UITabContentContainer');
	var uiTabContent = DOMUtil.findFirstDescendantByClass(uiTabContentContainer, 'div', 'UITabContent');
	var menuitems = DOMUtil.findDescendantsByClass(uiTabContent, 'div', 'LeftMenu');
	for(var i = 0; i < menuitems.length; i++) {
		menuitems[i].onclick = function() {
			 eXo.ecm.UIFormGeneratorPortlet.renderComponent(this.getAttribute('elementType'));	
		}
	}
};

UIFormGeneratorPortlet.prototype.renderComponent = function(typeComp) {
	var formGenerator 	= "";
	var fieldComponent 	= "";
	var advancedOption 	= "";
	var multivalue 		= false;

//============================================ Begin of render component ===============================================	

	switch(typeComp){
		case "label"		:
			fieldComponent  +=		"<td class='FieldLabel' value='Label'>Label</td>";
			fieldComponent  +=		"<td class='FieldComponent'></td>";
			break;
		case "input"		: 
			fieldComponent  +=		"<td class='FieldLabel' value='Input Text'>Input field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><input type='text' class='InputText' value=''/></td>";
			break;
		case "textarea"	:
			fieldComponent  +=		"<td class='FieldLabel' value='Textarea'>Textarea field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><textarea class='Textarea'>Textarea value</textarea></td>";
			break;			
		case "wysiwyg"		:
			if(!eXo.ecm.UIFormGeneratorPortlet.countFCK) eXo.ecm.UIFormGeneratorPortlet.countFCK = 1;
			fieldComponent  +=		"<td class='FieldLabel' value='WYSIWYG'>WYSIWYG field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><textarea class='Textarea' id='RichTextEditorContent_"+eXo.ecm.UIFormGeneratorPortlet.countFCK+"'>WYSIWYG value</textarea></td>";

			advancedOption  +=	"<tr>";
			advancedOption  +=		"<td class='FieldLabel'>Advance Options</td>";
			advancedOption  +=		"<td class='FileComponent'>";
			advancedOption  += 			"Toolbar: <select class='SelectBox'><option value='toolbar:SuperBasicWCM'>SuperBasicWCM</option><option value='toolbar:BasicWCM'>BasicWCM</option><option value='toolbar:CompleteWCM'>CompleteWCM</option><option value='toolbar:Basic'>Basic</option></select>";
			advancedOption  +=		"</td>";
			advancedOption  +=	"</tr>";
			break;			
		case "select"		: 
			fieldComponent  +=		"<td class='FieldLabel' value='Select'>Select field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><select class='SelectBox'><option idx='1' value='option1'>Option 1</option></select></td>";
			multivalue		= true;
			break;			
		case "checkbox"	: 
			fieldComponent  +=		"<td class='FieldLabel' value='Checkbox'>Checkbox field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><div class='CheckboxButton'><input type='checkbox' class='CheckBox' value='checkbox1'/><span style='padding : 0 5px 0 19px; display:block; line-height:12px'>Checkbox 1</span><div style='clear:left'></div></div></td>";
			break;						
		case "radio"		: 
		  var groupId = "radiogroup" + UIFormGeneratorPortlet.prototype.getRandomInt(1,100);		  
		  while(document.getElementsByName(groupId) > 0) {
		    groupId = "radiogroup" + UIFormGeneratorPortlet.prototype.getRandomInt(1,100);
		  }
			fieldComponent  +=		"<td class='FieldLabel' value='Radio'>Radio field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><div class='RadioButton' idx='1'><input type='radio' name='"+groupId+"' class='Radio' value='radio1'/><span style='padding : 0 5px 0 19px; display:block; line-height:12px'>Radio 1</span><div style='clear:left'></div></div></td>";
			multivalue		= true;
			break;			
		case "datetime"	: 
			fieldComponent  +=		"<td class='FieldLabel' value='DateTime'>Datetime field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><input type='text' class='InputText' value='Datetime value'/></td>";
			
			advancedOption  +=	"<tr>";
			advancedOption  +=		"<td class='FieldLabel'>Advance Options</td>";
			advancedOption  +=		"<td class='FileComponent'>";
			advancedOption  += 			"Format: <select><option>dd/mm/yyyy</option><option>dd-mm-yyyy</option></select>";
			advancedOption  +=		"</td>";
			advancedOption  +=	"</tr>";

			break;
		case "upload"		: 
			fieldComponent  +=		"<td class='FieldLabel' value='Upload'>Upload field</td>";
			fieldComponent  +=		"<td class='FieldComponent'><input type='file' class='Upload' disabled='disabled'/><img src='/eXoResources/skin/sharedImages/Blank.gif' alt='Upload' class='UploadButton'/></td>";
			break;
	}

	formGenerator  +=		"<div class='TopContentBoxStyle'>";
	formGenerator  +=			"<div class='UIForm UIFormEditBox'>";
	formGenerator  +=				"<div class='HorizontalLayout'>";
	formGenerator  +=					"<div class='FormContainer'>";
	formGenerator  +=						"<table class='UIFormGrid'>";
	formGenerator  +=							"<tr>";
	formGenerator  += 								fieldComponent;
	formGenerator  +=								"<td class='FieldIcon'>";
	formGenerator  +=									"<div class='EditBox'>";
	formGenerator  +=										"<a class='ControlIcon DeleteIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.removeComponent(this);' title='Click here to remove this component'><span></span></a>";
	formGenerator  +=										"<a class='ControlIcon EditIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.showEditBox(this);' title='Click here to edit property'><span></span></a>";
	formGenerator  +=									"</div>";
	formGenerator  +=									"<a class='ControlIcon DownIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.moveDownElement(this);' title='Move down component'><span></span></a>";
	formGenerator  +=									"<a class='ControlIcon UpIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.moveUpElement(this);' title='Move up component'><span></span></a>";
	formGenerator  +=									"<div class='ClearRight'><span></span></div>";
	formGenerator  +=								"</td>";
	formGenerator  +=							"</tr>";
	formGenerator  +=						"</table>";
	formGenerator  +=					"</div>";
	formGenerator  +=				"</div>";
	formGenerator  +=			"</div>";
	formGenerator  +=		"</div>";
	formGenerator  +=		"<div class='MiddleContentBoxStyle' style='display:none'>";
	formGenerator  +=			"<div class='UIForm UIFormEditBox'>";
	formGenerator  +=				"<div class='HorizontalLayout'>";
	formGenerator  +=					"<div class='FormContainer'>";
	formGenerator  +=						"<table class='UIFormGrid'>";
	formGenerator  +=							"<tr>";
	formGenerator  +=								"<td class='FieldLabel'>Field Label</td>";
	formGenerator  +=								"<td class='FieldComponent'>";
	formGenerator  +=									"<input type='text' class='InputText' onkeyup='eXo.ecm.UIFormGeneratorPortlet.updateLabel(this);'/>";
	formGenerator  +=								"</td>";
	formGenerator  +=							"</tr>";
	formGenerator  +=							"<tr>";
	formGenerator  +=								"<td class='FieldLabel'>Width</td>";	
	formGenerator  +=								"<td class='FieldComponent'>";
	formGenerator  +=									"<input type='number' value='' class='InputText' style='width: 50%; float:left;' onkeyup='eXo.ecm.UIFormGeneratorPortlet.updateWidth(this);' />";
	formGenerator  +=									"<div class='BoxRules'>";
	formGenerator  +=										"Rules: <input class='Requied' type='checkbox' onchange='eXo.ecm.UIFormGeneratorPortlet.updateRequired(this);'>Required</input>";
	formGenerator  +=									"</div>";
	formGenerator  +=								"</td>";
	formGenerator  +=								"<td class='FieldIcon'><span></span></td>";
	formGenerator  +=							"</tr>";
	formGenerator  +=							"<tr>";
	formGenerator  +=								"<td class='FieldLabel'>Height</td>";	
	formGenerator  +=								"<td class='FieldComponent'>";
	formGenerator  +=									"<input type='number' value='' class='InputText' style='width: 50%; float:left;' onkeyup='eXo.ecm.UIFormGeneratorPortlet.updateHeight(this);' />";
	formGenerator  +=								"</td>";
	formGenerator  +=								"<td class='FieldIcon'><span></span></td>";
	formGenerator  +=							"</tr>";		
	formGenerator  +=							"<tr>";
  formGenerator  +=               "<td class='FieldLabel'>Default Value</td>";
  formGenerator  +=               "<td class='FieldComponent' >";
  formGenerator  +=                 "<div class='DivIcon' idx='1'>";
  formGenerator  +=                   "<input type='text' class='InputText' onkeyup='eXo.ecm.UIFormGeneratorPortlet.updateValue(event);' idx='1'/ style=\"float:left;width:90%\">";
  if (multivalue) {
    formGenerator  +=                 "<a class='AddIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.addOption(this);'><span></span></a>";
    formGenerator  +=                 "<a class='RemoveIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.removeOption(this);'><span></span></a>";

  } else {
    formGenerator  +=                 "<span></span>";
  }
  formGenerator  +=                   "<div style='clear:left;'><span></span></div>";
  formGenerator  +=                 "</div>";
  formGenerator  +=               "</td>";
  formGenerator  +=             "</tr>";
  formGenerator  +=             advancedOption
	formGenerator  +=							"<tr>";
	formGenerator  +=								"<td class='FieldLabel'>Guidelines for User</td>";
	formGenerator  +=								"<td class='FieldComponent'><textarea class='Textarea' onkeyup='eXo.ecm.UIFormGeneratorPortlet.updateGuide(this);'></textarea></td>";
	formGenerator  +=								"<td class='FieldIcon'><span></span></td>";
	formGenerator  +=							"</tr>";
	formGenerator  +=						"</table>";
	formGenerator  +=					"</div>";
	formGenerator  +=				"</div>";
	formGenerator  +=			"</div>";
	formGenerator  +=		"</div>";		

//============================================ End of render component ===============================================

	var node = document.createElement('div');
	node.innerHTML = formGenerator;
	node.className = 'BoxContentBoxStyle';
	node.setAttribute('typeComponent', typeComp);
	document.getElementById('MiddleCenterViewBoxStyle').appendChild(node);

if(typeComp == 'wysiwyg' && !CKEDITOR.instances['RichTextEditorContent']) {
		var idEditor = 	'RichTextEditorContent_'+ eXo.ecm.UIFormGeneratorPortlet.countFCK;
		CKEDITOR.replace( idEditor,
    {
        toolbar : 'BasicWCM'
    });
	}
		eXo.ecm.UIFormGeneratorPortlet.countFCK++;
};


UIFormGeneratorPortlet.prototype.showEditBox = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var parentNode = DOMUtil.findAncestorByClass(obj, "TopContentBoxStyle");
	var boxContent = DOMUtil.findNextElementByTagName(parentNode, "div");
	if(boxContent && boxContent.style.display !='block') {
		boxContent.style.display = 'block';
	} else {
		boxContent.style.display = 'none';
	}
};

UIFormGeneratorPortlet.prototype.removeComponent = function(obj) {
	var parentNode = eXo.core.DOMUtil.findAncestorByClass(obj, "BoxContentBoxStyle");
	if(parentNode) {
		var confirmDelete = confirm("Are you sure to remove?");
		if(confirmDelete == true) {
				document.getElementById('MiddleCenterViewBoxStyle').removeChild(parentNode);			
		} else {
			return;
		}
	}
};

UIFormGeneratorPortlet.prototype.moveDownElement = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var parentNode = DOMUtil.findAncestorByClass(obj, "BoxContentBoxStyle");
	var middContainer = document.getElementById('MiddleCenterViewBoxStyle');
	if(!middContainer || !parentNode) return;
	var tmpNode = '';
	nextElt = DOMUtil.findNextElementByTagName(parentNode, 'div');
	if(nextElt) {
		tmpNode = nextElt.cloneNode(true);
		middContainer.removeChild(nextElt);
		middContainer.insertBefore(tmpNode, parentNode);
	}
};

UIFormGeneratorPortlet.prototype.moveUpElement = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var parentNode = DOMUtil.findAncestorByClass(obj, "BoxContentBoxStyle");
	var middContainer = document.getElementById('MiddleCenterViewBoxStyle');
	if(!middContainer || !parentNode) return;
	previousElt = DOMUtil.findPreviousElementByTagName(parentNode, 'div');
	if(!previousElt) return;
	var tmpNode = parentNode.cloneNode(true);
	middContainer.removeChild(parentNode);
	middContainer.insertBefore(parentNode, previousElt);
};

UIFormGeneratorPortlet.prototype.updateLabel = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var parentNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
	var labelNode = DOMUtil.findFirstDescendantByClass(parentNode, 'td', 'FieldLabel');
	labelNode.innerHTML = obj.value;
};

UIFormGeneratorPortlet.prototype.updateWidth = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var width = '';
	if (obj.value == '') {
		width = null;
	} else if (isNaN(parseFloat(obj.value))) {
		alert('Number only');
		obj.value = '';
		return false;
	} else {
		width = obj.value + 'px';
	}

	var parentNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
	var containerNode = DOMUtil.findFirstDescendantByClass(parentNode, 'div', 'TopContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldComponent');
	if(componentNode) {
		var inputNode = componentNode.childNodes[0];
		if (inputNode) inputNode.style.width = width;
	}
};

UIFormGeneratorPortlet.prototype.updateHeight = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var height = '';
	if (obj.value == '') {
		height = null;
	} else if (isNaN(parseFloat(obj.value))) {
		alert('Number only');
		obj.value = '';
		return false;
	} else {
		height = obj.value + 'px';
	}

	var parentNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
	var containerNode = DOMUtil.findFirstDescendantByClass(parentNode, 'div', 'TopContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldComponent');
	if(componentNode) {
		var inputNode = componentNode.childNodes[0];
		if (inputNode) inputNode.style.height = height;
	}
};

UIFormGeneratorPortlet.prototype.updateRequired = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var parentNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
	var containerNode = DOMUtil.findFirstDescendantByClass(parentNode, 'div', 'TopContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldComponent');
	var labelNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldLabel');	
	if (!componentNode) return false;
	if(obj.checked)	{
		labelNode.setAttribute('mandatory', 'true');
	} else {
		labelNode.setAttribute('mandatory', 'false');
	}
	var requiredNode = DOMUtil.getChildrenByTagName(componentNode, 'span')[0];
	if (!requiredNode) {
		requiredNode = document.createElement('span');
		requiredNode.style.color = 'red';
		requiredNode.innerHTML = ' *';
		componentNode.appendChild(requiredNode);
	} else {
		componentNode.removeChild(requiredNode);
	}
};

UIFormGeneratorPortlet.prototype.updateValue = function(evt) {
	var DOMUtil = eXo.core.DOMUtil;
	var srcEle = eXo.core.Browser.getEventSource(evt);
	if(!srcEle) return;
	var root = DOMUtil.findAncestorByClass(srcEle, 'BoxContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(root, 'div', 'TopContentBoxStyle');
	var eltName = DOMUtil.findFirstDescendantByClass(componentNode, 'td', 'FieldLabel').getAttribute('value');
	var srcIndex = parseInt(srcEle.getAttribute("idx"))-1;
	if(!eltName) return;
	switch(eltName) {
		case "Label" :
			var labelNode = DOMUtil.findFirstDescendantByClass(componentNode, 'td', 'FieldComponent');
			labelNode.innerHTML = srcEle.value;
			break;
		case "Input Text" :
			var inputNode = DOMUtil.findFirstDescendantByClass(componentNode, 'input', 'InputText');
			inputNode.value = srcEle.value;
			break;
		case "Select" :
			var selectNode = DOMUtil.findFirstDescendantByClass(componentNode, 'select', 'SelectBox');
			var fieldNode = DOMUtil.findAncestorByClass(srcEle, 'FieldComponent');
			var inputNodes = DOMUtil.getChildrenByTagName(fieldNode, 'input');
			if (selectNode.options.length>srcIndex) {
				selectNode.options[srcIndex].value = srcEle.value;
				selectNode.options[srcIndex].innerHTML = srcEle.value;
			}
			break;
		case "Textarea" :	
			var textarea = DOMUtil.findFirstDescendantByClass(componentNode, 'textarea', 'Textarea');
			textarea.value = srcEle.value;
			break
		case "WYSIWYG" :
			var eltComp = DOMUtil.findFirstDescendantByClass(componentNode, 'td', 'FieldComponent');
			var txtArea = DOMUtil.findFirstChildByClass(eltComp, "textarea", "Textarea");
			var iEditor = txtArea.id;
			var editor = eval('CKEDITOR.instances.'+iEditor);
			editor.setText(srcEle.value);		
			break;
		case "upload" : 
			break;
		case "Radio"  :
			var fieldComponent = DOMUtil.findFirstDescendantByClass(componentNode, "td", "FieldComponent");
			var radioNodes = DOMUtil.findDescendantsByClass(fieldComponent, 'div', 'RadioButton');
			var fieldNode = DOMUtil.findAncestorByClass(srcEle, 'FieldComponent');
			var inputList = DOMUtil.findDescendantsByClass(fieldNode, 'input', 'InputText');
      for(var i = 0 ; i < inputList.length; i++) {
        if(inputList[i] == srcEle) {
          var radioInputNode = DOMUtil.findFirstDescendantByClass(radioNodes[i], 'input', 'Radio');
          if (srcEle.value.length>0) radioInputNode.value = srcEle.value;
          var radioTextNode = DOMUtil.findDescendantsByTagName(radioNodes[i], 'span')[0];
          if (srcEle.value.length>0) radioTextNode.innerHTML = srcEle.value;
        } 
      }
			break;	
		case "Checkbox" :
			var fieldComponent = DOMUtil.findFirstDescendantByClass(componentNode, "td", "FieldComponent");
			var checkboxNodes = DOMUtil.findDescendantsByClass(fieldComponent, 'div', 'CheckboxButton');
			var fieldNode = DOMUtil.findAncestorByClass(srcEle, 'FieldComponent');
			var inputList = DOMUtil.findDescendantsByClass(fieldNode, 'input', 'InputText');
			for(var i = 0 ; i < inputList.length; i++) {
				if(inputList[i] == srcEle) {
					var chkInputNode = DOMUtil.findFirstDescendantByClass(checkboxNodes[i], 'input', 'CheckBox');
					chkInputNode.value = srcEle.value;
					var chkTextNode = DOMUtil.findDescendantsByTagName(checkboxNodes[i], 'span')[0];
					chkTextNode.innerHTML = srcEle.value;
				}
			}
			break;	
	}
};

UIFormGeneratorPortlet.prototype.updateGuide = function(objGuide) {
	var DOMUtil = eXo.core.DOMUtil;
	var root = DOMUtil.findAncestorByClass(objGuide, 'BoxContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(root, 'div', 'TopContentBoxStyle');
	var fieldLabel = DOMUtil.findFirstDescendantByClass(componentNode, 'td', 'FieldLabel');
	fieldLabel.setAttribute("desc", objGuide.value);
};
// This function inserts newNode after referenceNode
// when the second parameter of insertBefore is null then the newNode is appended to the end of the parentNode
UIFormGeneratorPortlet.prototype.insertAfter = function( referenceNode, newNode ){
    referenceNode.parentNode.insertBefore( newNode, referenceNode.nextSibling );
}
//After being removed all the elements need to update their indexes. This function does that!
UIFormGeneratorPortlet.prototype.updateNodeIndex = function( elementNode, index ){//alert("called");
	for(var i=parseInt(index); i< elementNode.length; i++){					
		elementNode[i].setAttribute("idx",parseInt(i)+1);				
	}		
}

UIFormGeneratorPortlet.prototype.addOption = function(obj) {		
	var DOMUtil = eXo.core.DOMUtil;
	var ancestorNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
	var containerNode = DOMUtil.findFirstDescendantByClass(ancestorNode, 'div', 'TopContentBoxStyle');
	var componentNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldComponent');	
	var selectNode = DOMUtil.findFirstDescendantByClass(componentNode, 'select', 'SelectBox');
	var rowNode = DOMUtil.findAncestorByTagName(obj, 'tr');
	var brotherNode = DOMUtil.findFirstDescendantByClass(rowNode, 'td', 'FieldComponent');	
	var brotherChildNodes = DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText');
	var radioContainer = DOMUtil.findDescendantsByClass(containerNode, 'div', 'RadioButton');						
	var upperIconNode = DOMUtil.findAncestorByClass(obj, 'DivIcon');		
	var currentIndex = upperIconNode.getAttribute("idx");
	var index = parseInt(currentIndex)+1;	
	
	var strRNode = "<input onkeyup=\"eXo.ecm.UIFormGeneratorPortlet.updateValue(event);\" type='text' class='InputText' idx='" + index + "'/ style=\"float:left;width:90%\">";

	strRNode  +="<a class='AddIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.addOption(this);'><span></span></a>";
	strRNode  +="<a class='RemoveIcon' onclick='eXo.ecm.UIFormGeneratorPortlet.removeOption(this);'><span></span></a>";
	strRNode  +="<div style='clear:left;'><span></span></div>";
	var rNode = document.createElement('div');
	rNode.className="DivIcon";
	rNode.setAttribute("idx",index);
	rNode.innerHTML=strRNode;
	eXo.ecm.UIFormGeneratorPortlet.insertAfter(upperIconNode,rNode);  

	var arrayInputTexts =DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText'); 
	var arrayDivIcons = DOMUtil.findDescendantsByClass(brotherNode, 'div', 'DivIcon');
	eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(arrayInputTexts,index);
	eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(arrayDivIcons,index);
	switch(ancestorNode.getAttribute("typeComponent")) {		
		case "select" :			
			var optionNode = document.createElement('option');
			optionNode.setAttribute("idx", index);
			optionNode.value = "Option"+index;
			optionNode.innerHTML = "Option"+index;		
			if (currentIndex <= selectNode.length ) {
				eXo.ecm.UIFormGeneratorPortlet.insertAfter(selectNode[currentIndex-1],optionNode);
			}
			eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(selectNode,index);		
			for(var i=0; i< arrayInputTexts.length; i++){
				if(arrayInputTexts[i].value=="" || arrayInputTexts[i].value.length <1) {
					selectNode[i].value="Option"+selectNode[i].getAttribute("idx");				
					selectNode[i].text="Option"+selectNode[i].getAttribute("idx");
				}
			}						
			break;
		case "radio" :		
		  var radioElement = DOMUtil.findFirstDescendantByClass(ancestorNode, 'input', 'Radio');
		  var radioName = radioElement.getAttribute('name');		
			var radioNode  = document.createElement("div");
			radioNode.setAttribute("idx",index);
			radioNode.innerHTML = '<input type="radio" name="'+radioName+'" class="Radio" value="radio'+index+'" /><span style="padding : 0 5px 0 19px; display:block; line-height:12px">Radio '+index+'</span><div style="clear:left"></div>';
			radioNode.className = "RadioButton";					
			if (currentIndex <= radioContainer.length ) {
				eXo.ecm.UIFormGeneratorPortlet.insertAfter(radioContainer[currentIndex-1],radioNode);
      		}
			var rContainer = DOMUtil.findDescendantsByClass(containerNode, 'div', 'RadioButton');		
			var arrInputTexts =DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText');				
			eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(rContainer,index);								
			for(var i=0; i< rContainer.length; i++){						
				if(arrInputTexts[i].value=="" || arrInputTexts[i].value.length <1) {											
					rContainer[i].firstChild.value="radio"+rContainer[i].getAttribute("idx");				
					rContainer[i].firstChild.nextSibling.innerHTML="Radio "+rContainer[i].getAttribute("idx");
				}
			}						
			break;
		case "checkbox" :
			var checkboxNode  = document.createElement("div");
			checkboxNode.innerHTML = '<input type="checkbox" class="CheckBox" value="checkbox'+index+'" /><span style="padding : 0 5px 0 19px; display:block; line-height:12px">Checkbox '+index+'</span><div style="clear:left"></div>';
			checkboxNode.className = "CheckboxButton";
			componentNode.appendChild(checkboxNode);	
			break;
	}			
};
UIFormGeneratorPortlet.prototype.removeOption = function(obj) { 

  var DOMUtil = eXo.core.DOMUtil;
  var ancestorNode = DOMUtil.findAncestorByClass(obj, 'BoxContentBoxStyle');
  var containerNode = DOMUtil.findFirstDescendantByClass(ancestorNode, 'div', 'TopContentBoxStyle');
  var componentNode = DOMUtil.findFirstDescendantByClass(containerNode, 'td', 'FieldComponent');  
  var selectNode = DOMUtil.findFirstDescendantByClass(componentNode, 'select', 'SelectBox');
  var rowNode = DOMUtil.findAncestorByTagName(obj, 'tr');
  var brotherNode = DOMUtil.findFirstDescendantByClass(rowNode, 'td', 'FieldComponent');  
  
  var radioContainer = DOMUtil.findDescendantsByClass(containerNode, 'div', 'RadioButton');           
  var upperIconNode = DOMUtil.findAncestorByClass(obj, 'DivIcon');    
  var brotherChildNodes = DOMUtil.findDescendantsByClass(brotherNode, 'div', 'DivIcon');
  var currentIndex = upperIconNode.getAttribute("idx");
  var index = parseInt(currentIndex)+1;
  
  for(var j =0; j< brotherChildNodes.length; j++){
    if(brotherChildNodes[j].getAttribute("idx")==currentIndex){
      brotherChildNodes[j].parentNode.removeChild(brotherChildNodes[j]);
      break;
    }       
  } 
  
  switch(ancestorNode.getAttribute("typeComponent")) {    
    case "select" :           
      if (selectNode.length>currentIndex-1) {
      	selectNode[0].parentNode.removeChild(selectNode[currentIndex-1]);
      }
      eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(selectNode,0); 
      var arrInputTexts = DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText');        
      for(var i=0; i< arrInputTexts.length; i++){
        if(arrInputTexts[i].value=="" || arrInputTexts[i].value.length <1) {
          selectNode[i].value="Option"+selectNode[i].getAttribute("idx");       
          selectNode[i].text="Option"+selectNode[i].getAttribute("idx");
        }
      }           
      break;
    case "radio" :              
      if (radioContainer.length>currentIndex-1) {
        radioContainer[0].parentNode.removeChild(radioContainer[currentIndex-1]);
      }
      var rContainer = DOMUtil.findDescendantsByClass(containerNode, 'div', 'RadioButton');   
      var arrInputTexts =DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText');       
      eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(rContainer,0);               
      for(var i=0; i< rContainer.length; i++){            
        if(arrInputTexts[i].value=="" || arrInputTexts[i].value.length <1) {                      
          rContainer[i].firstChild.value="Radio "+rContainer[i].getAttribute("idx");        
          rContainer[i].firstChild.nextSibling.innerHTML="Radio "+rContainer[i].getAttribute("idx");
        }
      }           
      break;    
  } 

  var arrayInputTexts = DOMUtil.findDescendantsByClass(brotherNode, 'input', 'InputText');
  eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(arrayInputTexts,0);
  var arrayDivIcons = DOMUtil.findDescendantsByClass(brotherNode, 'div', 'DivIcon');
  eXo.ecm.UIFormGeneratorPortlet.updateNodeIndex(arrayDivIcons,0);
  
};
UIFormGeneratorPortlet.prototype.getStringJsonObject = function() {
	var DOMUtil = eXo.core.DOMUtil;
	var root = document.getElementById('MiddleCenterViewBoxStyle');
	var boxsContent = DOMUtil.findDescendantsByClass(root, 'div', 'BoxContentBoxStyle');
	var strJsonObject = '{ "inputs" : [';	
	for(var i = 0; i < boxsContent.length; i++) {
		strJsonObject += eXo.ecm.UIFormGeneratorPortlet.getProperties(boxsContent[i]);
		if(i != (boxsContent.length-1)) {
			strJsonObject += ',';
		}
	}
	strJsonObject += "]}";
	return	strJsonObject; 		
};

UIFormGeneratorPortlet.prototype.getProperties = function(comp) {
	var DOMUtil = eXo.core.DOMUtil;
	var strObject = '{';
	strObject += '"type":"'+comp.getAttribute("typeComponent")+'",';
	var topContent = DOMUtil.findFirstDescendantByClass(comp, 'div', 'TopContentBoxStyle');
	var fieldLabel = DOMUtil.findFirstDescendantByClass(topContent, 'td', 'FieldLabel');
	var defaultValue = fieldLabel.getAttribute('value'); 
	var nameComp = '';		
	if(fieldLabel && fieldLabel.innerHTML != '') {
		nameComp = fieldLabel.innerHTML;
	} else {
		nameComp = defaultValue;
	}
	
	strObject += '"name":"'+ encodeURIComponent(nameComp)+'",';
	switch(comp.getAttribute("typeComponent")) {
		case "input" :
			inputNode = DOMUtil.findFirstDescendantByClass(topContent, 'input', "InputText");
			var width	= inputNode.offsetWidth;
			var mandatory = fieldLabel.getAttribute('mandatory');
			var height  = inputNode.offsetHeight;
			strObject +=  '"value":"'+inputNode.value+'","width":'+width+',"mandatory":'+mandatory+',"height":'+height+',';
			break;
		case "label" :
			var labelNode = DOMUtil.findFirstDescendantByClass(topContent, 'td', 'FieldComponent');
			strObject +=  '"value":"' + labelNode.innerHTML + '","width":0,"mandatory":'+mandatory+',"height":0,';
			break;
		case "textarea" :
			var textareaNode = DOMUtil.findFirstDescendantByClass(topContent, 'textarea', "Textarea");
			var width	= textareaNode.offsetWidth;
			var mandatory = fieldLabel.getAttribute('mandatory');
			var height  = textareaNode.offsetHeight;
			strObject +=  '"value":"'+textareaNode.value+'","width":'+width+',"mandatory":'+mandatory+',"height":'+height+',';	
			break;
		case "wysiwyg" : 
			var midContent = DOMUtil.findNextElementByTagName(topContent, "div");
			var selectNode = DOMUtil.findFirstDescendantByClass(midContent, 'select', "SelectBox");
			strObject +=  '"value":"Type content here...","width":0,"mandatory":'+mandatory+',"height":0,"advanced":"'+selectNode.value+'",';	
			break;
		case "select" :
			var selectNode = DOMUtil.findFirstDescendantByClass(topContent, 'select', "SelectBox");
			var width	= selectNode.offsetWidth;
			var mandatory = fieldLabel.getAttribute('mandatory');
			var height  = selectNode.offsetHeight;
			strObject +=  '"value":"'+selectNode.value+'","width":'+width+',"mandatory":'+mandatory+',"height":'+height+',';		
			var options = DOMUtil.getChildrenByTagName(selectNode, 'option');
			var advOptions = '';	
			strObject += 	'"advanced":"';
			for(var i = 0; i < options.length; i++) {
				strObject += options[i].value;
				if(i != (options.length-1)) {
					strObject += ",";				
				}
			}
			strObject += '",';
			break;
		case "upload" :
			strObject +=  '"value":"null","width":0,"mandatory":'+mandatory+',"height":0,';	
			break;
		case "radio" :
		 var radioButtonList = DOMUtil.findDescendantsByClass(topContent, 'div', 'RadioButton');
			var strAdvanced = "";			
			for(var i = 0 ; i < radioButtonList.length; i++) {		 	
				var radioButton = radioButtonList[i];
				var inputList = DOMUtil.getChildrenByTagName(radioButton, 'input');							
				for(var j = 0 ; j < inputList.length; j++) {
					var radioLabel = DOMUtil.findNextElementByTagName(inputList[j], 'span');
					if ((radioLabel != null) && (radioLabel.innerHTML.length>0)) {
						strAdvanced += radioLabel.innerHTML + ",";
					} else {
						strAdvanced += inputList[j].value + ",";
					}
				}			
			}
			var mandatory = fieldLabel.getAttribute('mandatory');
			strObject += '"mandatory":'+mandatory+',';
			strObject += '"advanced":"' + strAdvanced + '",';
			break;
		case "checkbox" :
		 var checkboxButtonList = DOMUtil.findDescendantsByClass(topContent, 'div', 'CheckboxButton');
			var strAdvanced = "";			
			for(var i = 0 ; i < checkboxButtonList.length; i++) {		 	
				var checkboxButton = checkboxButtonList[i];
				var inputList = DOMUtil.getChildrenByTagName(checkboxButton, 'input');							
				for(var j = 0 ; j < inputList.length; j++) {
					var checkboxLabel = DOMUtil.findNextElementByTagName(inputList[j], 'span');
					if ((checkboxLabel != null) && (checkboxLabel.innerHTML.length>0)) {
						strAdvanced += checkboxLabel.innerHTML + ",";
					} else {
						strAdvanced += inputList[j].value + ",";
					}
				}			
			}
			var mandatory = fieldLabel.getAttribute('mandatory');
			strObject += '"mandatory":'+mandatory+',';
			strObject += '"advanced":"' + strAdvanced + '",';
			break;		
	}

	strObject += '"guideline":"'+fieldLabel.getAttribute('desc')+'"';
	strObject += "}";
	return strObject;
};

UIFormGeneratorPortlet.prototype.getRandomInt = function(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

UIFormGeneratorPortlet.prototype.submitForm = function() {
	var strJsonObject = eXo.ecm.UIFormGeneratorPortlet.getStringJsonObject();
	var inputHidden = document.getElementById("UIFormGeneratorJsonObjectStringInput");
	if(inputHidden) inputHidden.value = "" + strJsonObject;
};

eXo.ecm.UIFormGeneratorPortlet = new UIFormGeneratorPortlet();

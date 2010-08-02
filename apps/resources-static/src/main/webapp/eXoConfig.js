﻿﻿﻿/*
	eXo config plugins
*/

CKEDITOR.eXoPath = CKEDITOR.basePath.substr(0, CKEDITOR.basePath.indexOf("ckeditor/"));

// config to add custom plugin	
(function() {CKEDITOR.plugins.addExternal('content',CKEDITOR.eXoPath+'eXoPlugins/content/','plugin.js');})();
(function() {CKEDITOR.plugins.addExternal('insertGadget',CKEDITOR.eXoPath+'eXoPlugins/insertGadget/','plugin.js');})();
(function() {CKEDITOR.plugins.addExternal('insertPortalLink',CKEDITOR.eXoPath+'eXoPlugins/insertPortalLink/','plugin.js');})();

CKEDITOR.editorConfig = function( config ){
	config.resize_enabled = false; // config to disable editor resizing in CKEDITOR
	config.extraPlugins = 'content,insertGadget,insertPortalLink';
	config.toolbarCanCollapse = false;
	//config.uiColor = '#AADC6E';
	config.toolbar_Default = [
		['Source','Templates'],
		['Cut','Copy','PasteText','-','SpellCheck'],
		['Undo','Redo','-','RemoveFormat'],
		'/',
		['Bold','Italic','Underline','StrikeThrough'],
		['OrderedList','UnorderedList'],
		['Link','Unlink','Anchor'],
		['Image','Flash','Table','SpecialChar'],
		['TextColor','BGColor'],
		['FitWindow','ShowBlocks'],
		['Style','FontFormat','FontName','FontSize']
	] ;

	config.toolbar_Basic = [
		['Source','-','Bold','Italic','Underline','StrikeThrough','-','OrderedList','UnorderedList','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
		['Blockquote','-','Link','Unlink','-','FitWindow','ShowBlocks'],	
		['Style','FontFormat','FontName','FontSize','-','Maximize']
	] ;

	config.toolbar_CompleteWCM = [
		['Source','Templates','-','FitWindow','ShowBlocks'],
		['Cut','Copy','PasteText','-','SpellCheck','-','Undo','Redo'],
		['insertGadget.btn','Flash','Table','SpecialChar', 'content.btn'], 
		'/',	['Bold','Italic','Underline','StrikeThrough','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull','-','OrderedList','UnorderedList','-','TextColor','BGColor','-','RemoveFormat'],
		['Link','insertPortalLink.btn','Unlink','Anchor'],['Style','FontFormat','FontName','FontSize']
	] ;

	config.toolbar_BasicWCM = [
		['Source','-','Bold','Italic','Underline','StrikeThrough','-','OrderedList','UnorderedList','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
		['Blockquote','-','Link','Unlink','insertPortalLink.btn','content.btn','-','FitWindow','ShowBlocks'],	
		['Style','FontFormat','FontName','FontSize']
	] ;

	config.toolbar_SuperBasicWCM = [
		   ['Source','-','Bold','Italic','Underline'],
		   ['-','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
		   ['-','Link','Unlink','insertPortalLink.btn','insertGadget.btn','content.btn'],	
	] ;
};

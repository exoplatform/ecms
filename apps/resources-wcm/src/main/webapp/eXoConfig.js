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
	config.language = eXo.env.portal.language || 'en';
	//config.uiColor = '#AADC6E';
	config.toolbar_Default = [
		['Source','Templates'],
		['Cut','Copy','PasteText','-','SpellCheck'],
		['Undo','Redo','-','RemoveFormat'],
		'/',
		['Bold','Italic','Underline','Strike'],
		['NumberedList','BulletedList'],
		['Link','Unlink','Anchor'],
		['Image','Flash','Table','SpecialChar'],
		['TextColor','BGColor'],
		['Maximize', 'ShowBlocks'],
		['Style','Format','Font','FontSize']
	] ;

	config.toolbar_Basic = [
		['Source','-','Bold','Italic','Underline','Strike','-','NumberedList','BulletedList','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
		'/',
		['Blockquote','-','Link','Unlink', 'ShowBlocks'],		
		['Style','Format','Font','FontSize','-','Maximize']
	] ;

	config.toolbar_CompleteWCM = [
		['Source','Templates','ShowBlocks'],
		['Cut','Copy','PasteText','-','SpellCheck','-','Undo','Redo'],
		['insertGadget.btn','Flash','Table','SpecialChar', 'content.btn', 'Image'], 
		'/',	
		['Bold','Italic','Underline','Strike','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','-','NumberedList','BulletedList','-','TextColor','BGColor','-','RemoveFormat'],
		['Link','insertPortalLink.btn','Unlink','Anchor'],
		'/',
		['Style','Format','Font','FontSize', '-' ,'Maximize']
	] ;
	
	config.toolbar_BasicWCM = [
		['Source','-','Bold','Italic','Underline','Strike'],
    ['-','NumberedList','BulletedList','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','Blockquote'],
    '/',
		['-','Link','Unlink','insertPortalLink.btn','content.btn', 'Image'],
    ['-','Maximize','ShowBlocks','Style','Format','Font','FontSize']
	] ;

	config.toolbar_SuperBasicWCM = [
		   ['Source','-','Bold','Italic','Underline'],
		   ['-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
		   ['-','Link','Unlink','insertPortalLink.btn','insertGadget.btn','content.btn', 'Image'],	
	] ;
};

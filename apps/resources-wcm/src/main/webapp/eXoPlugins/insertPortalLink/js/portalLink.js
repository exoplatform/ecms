﻿﻿﻿﻿﻿﻿﻿﻿/*
Copyright (c) 2003-2010, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

var curInstance = '';
CKEDITOR.dialog.add( 'insertPortalLink.dlg', function( editor )
{
	curInstance = editor.name;
	var title = '';
	return {
		title : editor.lang.WCMInsertPortalLinkDialogTitle,
		minWidth : 390,
		minHeight : 230,
		contents : [
			{
				id : 'tab1',
				label : '',
				title : '',
				expand : true,
				padding : 0,
				elements :
				[
					{
						type : 'html',
						html :
							'<style type="text/css">' +
									'.ck_about_link .uIFormGrid {'+
										'margin:auto;'+
										'width:auto;'+
									'}'+

									'.ck_about_link .fieldLabel,.ck_about_link .fieldComponent {'+
										'padding:4px;'+
									'}'+
									
									'.ck_about_link .fieldComponent input {'+
										'width:230px;'+
										'margin: 0px;'+
									'}'+

									'.ck_about_link .uiAction .actionContainer {'+
										'margin:auto;'+
										'width:auto;'+
									'}'+		
									'.cke_reset_all .uiAction {'+
										'padding: 15px 0 5px;'+
										'text-align: center;'+
									'}'+									
									
									'.cke_reset_all  .btn {' +		
										'background-color: #F9F9F9;'+
										'background-image: linear-gradient(to bottom, #FFFFFF, #F1F1F1);'+
										'background-repeat: repeat-x;'+
										'border: 1px solid #BBBBBB;'+
										'border-radius: 4px 4px 4px 4px;'+
										'box-shadow: 0 1px 0 rgba(255, 255, 255, 0.2) inset, 0 1px 2px rgba(0, 0, 0, 0.05);'+
										'color: #333333;'+
										'font-size: 13px;'+
										'line-height: 18px;'+
										'text-shadow: 0 1px 1px rgba(255, 255, 255, 0.75);'+
										'padding: 4px 12px;'+
										'text-align: center;'+
										'vertical-align: middle;'+
										'cursor: pointer;'+
										'display: inline-block;'+
									'}'+
							'</style>' +
							'<div class="cke_about_container">' +
								'<div class="ck_about_link">'+
								'<table class="uIFormGrid">' +
									'<tbody>' +
										'<tr>' +
											'<td class="fieldLabel">'+
												'<label for="inputTitle">' + editor.lang.WCMInsertPortalLinkInputTitle + '</label>'+
											'</td>'+
											'<td colspan="2" class="fieldComponent">'+
												'<input type="text" id="inputTitle" value="'+ title +'" />'+
											'</td>'+
										'</tr>'+
										'<tr>'+
											'<td class="fieldLabel">'+
												'<label >' + editor.lang.WCMInsertPortalLinkInputUrl + '</label>'+
											'</td>'+
											'<td class="fieldComponent">'+
												'<input  type="text" id="txtUrl" />'+
											'</td>'+
											'<td class="fieldComponent">'+
												'<div  class="uiAction">'+
													'<table align="center" class="actionContainer">'+
														'<tbody>'+
															'<tr>'+
																'<td align="center">'+
																	'<a class="btn" onclick="getPortalLink();"  src="http://www.w3.org/1999/xhtml">'+
																		editor.lang.WCMInsertPortalLinkButtonGet +
																	'</a>'+
																					
																'</td>'+
															'</tr>'+
														'</tbody>'+
													'</table>'+
												'</div>'+
											'</td>'+
										'</tr>'+
		  						'</tbody>'+
								'</table>'+
								
								'<div class="uiAction ">'+
									'<a fcklang="WCMInsertPortalLinkButtonSave" class="btn"  onclick="addURL();"  src="http://www.w3.org/1999/xhtml">'+
										editor.lang.WCMInsertPortalLinkButtonSave +
									'</a>'+	 								
									'<a class="btn"  onclick="previewLink();" src="http://www.w3.org/1999/xhtml">'+
										editor.lang.WCMInsertPortalLinkButtonPreview +
									'</a>'+								             						
					      				
	    					'</div>'+
							'</div>'+
						'</div>'
					}
				]
			}
		],
		onShow : function() {
			title = editor.titleLink ;
			document.getElementById("inputTitle").value = title ;
		},
		buttons : [ CKEDITOR.dialog.cancelButton ],
		onCancel : function() {
			CKEDITOR.dialog.getCurrent().hide();
		}
		};
} );

function getPortalLink() {
	var sOptions = "toolbar=no,status=no,resizable=yes,dependent=yes,scrollbars=yes,width=800,height=600" ;
	var newWindow = window.open("/eXoWCMResources/eXoPlugins/insertPortalLink/insertPortalLink.html?type=PortalLink", "WCMInsertPortalLink", sOptions );
	newWindow.focus();
}

function previewLink() {
	var sOptions = "toolbar=no, status=no, resizable=yes, dependent=yes, scrollbars=yes,width=800,height=800";
	var url = document.getElementById("txtUrl").value;
	if (url) window.open(url, "", sOptions);
}

function addURL() {
	var tLink = document.getElementById("inputTitle").value;
	var url = document.getElementById("txtUrl").value;
	if (url == "") {
		alert("Field URL is not empty"); 
		return;
	}
	if(tLink == "" ) {
		var newTag = '<a href="'+url+'" style="color:blue;">'+url+'</a>';
	} else {
		var newTag = '<a href="'+url+'" style="color:blue;">'+tLink+'</a>';
	}
	CKEDITOR.instances[curInstance].insertHtml(newTag);
	CKEDITOR.dialog.getCurrent().hide();
}

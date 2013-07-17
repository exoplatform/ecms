﻿/*
Copyright (c) 2003-2011, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

(function()
{
	var defaultToPixel = CKEDITOR.tools.cssLength;

	var commitValue = function( data )
	{
		var id = this.id;
		if ( !data.info )
			data.info = {};
		data.info[id] = this.getValue();
	};

	/* get the value of a multi-value attribute (key) in a stylesheet string 
	 * Ex: 
	 *   - styleString: 	"border: 10px solid rgb(0, 0, 0); padding: 10px;"
	 *   - key: 		border
	 *   - valueNum: 	1
	 *   - return value: 	10px
	 */	
        var getValueFromMultiValuesStyle = function (styleString, key, valueNum) {
            var valuesArray = styleString.split(";");
            for (i = 0; i < valuesArray.length; i++) {
                if (valuesArray[i].split(":")[0].replace(" ", "") == key) return (valuesArray[i].split(":")[1].split(" ")[valueNum].replace("px", ""));
            }
        };
        
	/* get the value of a single-value attribute (key) in a stylesheet string 
	 * Ex: 
	 *   - styleString: 	"border: 10px solid rgb(0, 0, 0); padding: 10px;"
	 *   - key: 		padding
	 *   - return value: 	10px
	 */
        var getValueFromStyle = function (styleString, key) {
            var valuesArray = styleString.split(";");
            for (i = 0; i < valuesArray.length; i++) {
                if (valuesArray[i].split(":")[0].replace(" ", "") == key) return valuesArray[i].split(":")[1].replace(" ", "").replace("px", "");
            }
        };

	function tableColumns( table )
	{
		var cols = 0, maxCols = 0;
		for ( var i = 0, row, rows = table.$.rows.length; i < rows; i++ )
		{
			row = table.$.rows[ i ], cols = 0;
			for ( var j = 0, cell, cells = row.cells.length; j < cells; j++ )
			{
				cell = row.cells[ j ];
				cols += cell.colSpan;
			}

			cols > maxCols && ( maxCols = cols );
		}

		return maxCols;
	}
	
  function Replace(expr,a,b) {

   var i=expr.indexOf(a);
   if(i>=0){
   var inter=expr.substring(i,expr.length);
   var j=inter.indexOf('px');
   var inter2=inter.substring(0,j);
   k=inter2.lastIndexOf(' ');
   var long=j-a.length;
   expr=expr.substring(0,i+k+1)+b+expr.substring(i+a.length+long,expr.length);
   }
      return expr
   }

	function tableDialog( editor, command )
	{
		var makeElement = function( name )
			{
				return new CKEDITOR.dom.element( name, editor.document );
			};

		var dialogadvtab = editor.plugins.dialogadvtab;

		return {
			title : editor.lang.table.title,
			minWidth : 310,
			minHeight : CKEDITOR.env.ie ? 310 : 280,

			onLoad : function()
			{
				var dialog = this;

				var styles = dialog.getContentElement( 'advanced', 'advStyles' );

				if ( styles )
				{
					styles.on( 'change', function( evt )
						{
							// Synchronize width value.
							var width = this.getStyle( 'width', '' ),
								txtWidth = dialog.getContentElement( 'info', 'txtWidth' );

							txtWidth && txtWidth.setValue( width, true );

							// Synchronize height value.
							var height = this.getStyle( 'height', '' ),
								txtHeight = dialog.getContentElement( 'info', 'txtHeight' );

							txtHeight && txtHeight.setValue( height, true );
						});
				}
			},

			onShow : function()
			{
				// Detect if there's a selected table.
				var selection = editor.getSelection(),
					ranges = selection.getRanges(),
					selectedTable = null;

				var rowsInput = this.getContentElement( 'info', 'txtRows' ),
					colsInput = this.getContentElement( 'info', 'txtCols' ),
					widthInput = this.getContentElement( 'info', 'txtWidth' ),
					heightInput = this.getContentElement( 'info', 'txtHeight' );

				if ( command == 'tableProperties' )
				{
					if ( ( selectedTable = selection.getSelectedElement() ) )
						selectedTable = selectedTable.getAscendant( 'table', true );
					else if ( ranges.length > 0 )
					{
						// Webkit could report the following range on cell selection (#4948):
						// <table><tr><td>[&nbsp;</td></tr></table>]
						if ( CKEDITOR.env.webkit )
							ranges[ 0 ].shrink( CKEDITOR.NODE_ELEMENT );

						var rangeRoot = ranges[0].getCommonAncestor( true );
						selectedTable = rangeRoot.getAscendant( 'table', true );
					}

					// Save a reference to the selected table, and push a new set of default values.
					this._.selectedElement = selectedTable;
				}

				// Enable or disable the row, cols, width fields.
				if ( selectedTable )
				{
					this.setupContent( selectedTable );
					rowsInput && rowsInput.disable();
					colsInput && colsInput.disable();
				}
				else
				{
					rowsInput && rowsInput.enable();
					colsInput && colsInput.enable();
				}

				// Call the onChange method for the widht and height fields so
				// they get reflected into the Advanced tab.
				widthInput && widthInput.onChange();
				heightInput && heightInput.onChange();
			},
			onOk : function()
			{
				var selection = editor.getSelection(),
					bms = this._.selectedElement && selection.createBookmarks();

				var table = this._.selectedElement || makeElement( 'table' ),
					me = this,
					data = {};

				this.commitContent( data, table );

				if ( data.info )
				{
					var info = data.info;

					// Generate the rows and cols.
					if ( !this._.selectedElement )
					{
						var tbody = table.append( makeElement( 'tbody' ) ),
							rows = parseInt( info.txtRows, 10 ) || 0,
							cols = parseInt( info.txtCols, 10 ) || 0;
						for ( var i = 0 ; i < rows ; i++ )
						{
							var row = tbody.append( makeElement( 'tr' ) );
							for ( var j = 0 ; j < cols ; j++ )
							{
								var cell = row.append( makeElement( 'td' ) );
								// set border & padding for cells
								var styleString = 'border:' + info.txtBorder + 'px solid #000000; padding:' + info.txtCellPad + 'px;';
								cell.$.style.cssText = styleString;
								if ( !CKEDITOR.env.ie )
									cell.append( makeElement( 'br' ) );
							}
						}
					} else { 	
						//in case: right click on the table, choose "Table Properties" command, edit some stylesheet value and then click OK						
						var rows = parseInt( info.txtRows, 10 ) || 0;
						var cols = parseInt( info.txtCols, 10 ) || 0;
						if (rows > 0 && cols > 0) {
							var cellNumber = rows * cols;

								for (var i = 0; i < cellNumber; i++) {
                  if (this._.selectedElement.$.getElementsByTagName('td')[i])
                  {
							       var styleString = this._.selectedElement.$.getElementsByTagName('td')[i].style.cssText;
								   if (navigator.userAgent.indexOf("MSIE 8") >= 0 || navigator.userAgent.indexOf("MSIE 7") >= 0) {
								   styleString = Replace (styleString.toLowerCase(),"border-top: ",info.txtBorder);
								   styleString = Replace (styleString.toLowerCase(),"border-right: ",info.txtBorder);
								   styleString = Replace (styleString.toLowerCase(),"border-left: ",info.txtBorder);
								   styleString = Replace (styleString.toLowerCase(),"border-bottom: ",info.txtBorder);
								   
								   styleString = Replace (styleString.toLowerCase(),"padding-top: ",info.txtCellPad);
								   styleString = Replace (styleString.toLowerCase(),"padding-right: ",info.txtCellPad);
								   styleString = Replace (styleString.toLowerCase(),"padding-left: ",info.txtCellPad);
								   styleString = Replace (styleString.toLowerCase(),"padding-bottom: ",info.txtCellPad);
								   }
								   else{
								   styleString = Replace (styleString,"border: ",info.txtBorder);
								   styleString = Replace (styleString,"padding: ",info.txtCellPad);
								   }
								   this._.selectedElement.$.getElementsByTagName('td')[i].style.cssText = styleString;
                  }
							}
						}
					}

					// Modify the table headers. Depends on having rows and cols generated
					// correctly so it can't be done in commit functions.

					// Should we make a <thead>?
					var headers = info.selHeaders;
					if ( !table.$.tHead && ( headers == 'row' || headers == 'both' ) )
					{
						var thead = new CKEDITOR.dom.element( table.$.createTHead() );
						tbody = table.getElementsByTag( 'tbody' ).getItem( 0 );
						var theRow = tbody.getElementsByTag( 'tr' ).getItem( 0 );

						// Change TD to TH:
						for ( i = 0 ; i < theRow.getChildCount() ; i++ )
						{
							var th = theRow.getChild( i );
							// Skip bookmark nodes. (#6155)
							if ( th.type == CKEDITOR.NODE_ELEMENT && !th.data( 'cke-bookmark' ) )
							{
								th.renameNode( 'th' );
								th.setAttribute( 'scope', 'col' );
							}
						}
						thead.append( theRow.remove() );
					}

					if ( table.$.tHead !== null && !( headers == 'row' || headers == 'both' ) )
					{
						// Move the row out of the THead and put it in the TBody:
						thead = new CKEDITOR.dom.element( table.$.tHead );
						tbody = table.getElementsByTag( 'tbody' ).getItem( 0 );

						var previousFirstRow = tbody.getFirst();
						while ( thead.getChildCount() > 0 )
						{
							theRow = thead.getFirst();
							for ( i = 0; i < theRow.getChildCount() ; i++ )
							{
								var newCell = theRow.getChild( i );
								if ( newCell.type == CKEDITOR.NODE_ELEMENT )
								{
									newCell.renameNode( 'td' );
									newCell.removeAttribute( 'scope' );
								}
							}
							theRow.insertBefore( previousFirstRow );
						}
						thead.remove();
					}

					// Should we make all first cells in a row TH?
					if ( !this.hasColumnHeaders && ( headers == 'col' || headers == 'both' ) )
					{
						for ( row = 0 ; row < table.$.rows.length ; row++ )
						{
							newCell = new CKEDITOR.dom.element( table.$.rows[ row ].cells[ 0 ] );
							newCell.renameNode( 'th' );
							newCell.setAttribute( 'scope', 'row' );
						}
					}

					// Should we make all first TH-cells in a row make TD? If 'yes' we do it the other way round :-)
					if ( ( this.hasColumnHeaders ) && !( headers == 'col' || headers == 'both' ) )
					{
						for ( i = 0 ; i < table.$.rows.length ; i++ )
						{
							row = new CKEDITOR.dom.element( table.$.rows[i] );
							if ( row.getParent().getName() == 'tbody' )
							{
								newCell = new CKEDITOR.dom.element( row.$.cells[0] );
								newCell.renameNode( 'td' );
								newCell.removeAttribute( 'scope' );
							}
						}
					}

					// Set the width and height.
					info.txtHeight ? table.setStyle( 'height', info.txtHeight ) : table.removeStyle( 'height' );
					info.txtWidth ? table.setStyle( 'width', info.txtWidth ) : table.removeStyle( 'width' );

					if ( !table.getAttribute( 'style' ) )
						table.removeAttribute( 'style' );
				}

				// Insert the table element if we're creating one.
				if ( !this._.selectedElement )
				{
					editor.insertElement( table );
					// Override the default cursor position after insertElement to place
					// cursor inside the first cell (#7959), IE needs a while.
					setTimeout( function()
						{
							var firstCell = new CKEDITOR.dom.element( table.$.rows[ 0 ].cells[ 0 ] );
							var range = new CKEDITOR.dom.range( editor.document );
							range.moveToPosition( firstCell, CKEDITOR.POSITION_AFTER_START );
							range.select( 1 );
						}, 0 );
				}
				// Properly restore the selection, (#4822) but don't break
				// because of this, e.g. updated table caption.
				else
					try { selection.selectBookmarks( bms ); } catch( er ){}
			},
			contents : [
				{
					id : 'info',
					label : editor.lang.table.title,
					elements :
					[
						{
							type : 'hbox',
							widths : [ null, null ],
							styles : [ 'vertical-align:top' ],
							children :
							[
								{
									type : 'vbox',
									padding : 0,
									children :
									[
										{
											type : 'text',
											id : 'txtRows',
											'default' : 3,
											label : editor.lang.table.rows,
											required : true,
											controlStyle : 'width:5em',
											validate : function()
											{
												var pass = true,
													value = this.getValue();
												pass = pass && CKEDITOR.dialog.validate.integer()( value )
													&& value > 0;
												if ( !pass )
												{
													alert( editor.lang.table.invalidRows );
													this.select();
												}
												return pass;
											},
											setup : function( selectedElement )
											{
												this.setValue( selectedElement.$.rows.length );
											},
											commit : commitValue
										},
										{
											type : 'text',
											id : 'txtCols',
											'default' : 2,
											label : editor.lang.table.columns,
											required : true,
											controlStyle : 'width:5em',
											validate : function()
											{
												var pass = true,
													value = this.getValue();
												pass = pass && CKEDITOR.dialog.validate.integer()( value )
													&& value > 0;
												if ( !pass )
												{
													alert( editor.lang.table.invalidCols );
													this.select();
												}
												return pass;
											},
											setup : function( selectedTable )
											{
												this.setValue( tableColumns( selectedTable ) );
											},
											commit : commitValue
										},
										{
											type : 'html',
											html : '&nbsp;'
										},
										{
											type : 'select',
											id : 'selHeaders',
											'default' : '',
											label : editor.lang.table.headers,
											items :
											[
												[ editor.lang.table.headersNone, '' ],
												[ editor.lang.table.headersRow, 'row' ],
												[ editor.lang.table.headersColumn, 'col' ],
												[ editor.lang.table.headersBoth, 'both' ]
											],
											setup : function( selectedTable )
											{
												// Fill in the headers field.
												var dialog = this.getDialog();
												dialog.hasColumnHeaders = true;

												// Check if all the first cells in every row are TH
												for ( var row = 0 ; row < selectedTable.$.rows.length ; row++ )
												{
													// If just one cell isn't a TH then it isn't a header column
													var headCell = selectedTable.$.rows[row].cells[0];
													if ( headCell && headCell.nodeName.toLowerCase() != 'th' )
													{
														dialog.hasColumnHeaders = false;
														break;
													}
												}

												// Check if the table contains <thead>.
												if ( ( selectedTable.$.tHead !== null) )
													this.setValue( dialog.hasColumnHeaders ? 'both' : 'row' );
												else
													this.setValue( dialog.hasColumnHeaders ? 'col' : '' );
											},
											commit : commitValue
										},
										{
											type : 'text',
											id : 'txtBorder',
											'default' : 1,
											label : editor.lang.table.border,
											controlStyle : 'width:3em',
											validate : CKEDITOR.dialog.validate['number']( editor.lang.table.invalidBorder ),
											setup : function( selectedTable )
											{
												// set value for txtBorder 
												var styleString = selectedTable.$.style.cssText.toLowerCase(); 

												if (navigator.userAgent.indexOf("MSIE 8") >= 0 || navigator.userAgent.indexOf("MSIE 7") >= 0) {

												    this.setValue(getValueFromMultiValuesStyle(styleString, 'border-top', 2));
												} else {
													this.setValue(getValueFromMultiValuesStyle(styleString, 'border', 1));
												}
											},
											commit : function( data, selectedTable )
											{
												// get value from txtBorder & set style for the table
												if (this.getValue()) {
													var styles = this.getDialog().getContentElement( 'advanced', 'advStyles' );
													styles && styles.updateStyle( 'border', this.getValue() + 'px solid #000000' ) ;
												}
												var id = this.id;
												if (!data.info) data.info = {};
												data.info[id] = this.getValue();
											}
										},
										{
											id : 'cmbAlign',
											type : 'select',
											'default' : '',
											label : editor.lang.common.align,
											items :
											[
												[ editor.lang.common.notSet , ''],
												[ editor.lang.common.alignLeft , 'left'],
												[ editor.lang.common.alignCenter , 'center'],
												[ editor.lang.common.alignRight , 'right']
											],
											onChange : function()
											{
												// update table style when we change the value of cmbAlign
												var styles = this.getDialog().getContentElement( 'advanced', 'advStyles' );
												if (this.getValue() == 'center') {
												    styles && styles.updateStyle( 'margin-left', 'auto' ) ;
												    styles && styles.updateStyle( 'margin-right', 'auto' );
												} else if (this.getValue() == 'right') {
												    styles && styles.updateStyle( 'margin-left', 'auto' );
												    styles && styles.updateStyle( 'margin-right', '0px' );
												} else {
												    styles && styles.updateStyle( 'margin-left', '0px' );
												    styles && styles.updateStyle( 'margin-right', '0px' );
												}
											},
											setup : function( selectedTable )
											{
												//set value for cmbAlign
												var left = selectedTable.getStyle( 'margin-left' );
												var right = selectedTable.getStyle( 'margin-right' );
												var alignValue = '';
												if (left == 'auto' && right == 'auto') {
												    alignValue = 'center';
												} else if (left == 'auto' && right != 'auto') {
												    alignValue = 'right';
												} else if (left != 'auto' && right == 'auto') {
												    alignValue = 'left';
												}
												this.setValue( alignValue || '' );
											},
											commit : commitValue
										}
									]
								},
								{
									type : 'vbox',
									padding : 0,
									children :
									[
										{
											type : 'hbox',
											widths : [ '5em' ],
											children :
											[
												{
													type : 'text',
													id : 'txtWidth',
													controlStyle : 'width:5em',
													label : editor.lang.common.width,
													title : editor.lang.common.cssLengthTooltip,
													'default' : 500,
													getValue : defaultToPixel,
													validate : CKEDITOR.dialog.validate.cssLength( editor.lang.common.invalidCssLength.replace( '%1', editor.lang.common.width ) ),
													onChange : function()
													{
														var styles = this.getDialog().getContentElement( 'advanced', 'advStyles' );
														styles && styles.updateStyle( 'width', this.getValue() );
													},
													setup : function( selectedTable )
													{
														var val = selectedTable.getStyle( 'width' );
														val && this.setValue( val );
													},
													commit : commitValue
												}
											]
										},
										{
											type : 'hbox',
											widths : [ '5em' ],
											children :
											[
												{
													type : 'text',
													id : 'txtHeight',
													controlStyle : 'width:5em',
													label : editor.lang.common.height,
													title : editor.lang.common.cssLengthTooltip,
													'default' : '',
													getValue : defaultToPixel,
													validate : CKEDITOR.dialog.validate.cssLength( editor.lang.common.invalidCssLength.replace( '%1', editor.lang.common.height ) ),
													onChange : function()
													{
														var styles = this.getDialog().getContentElement( 'advanced', 'advStyles' );
														styles && styles.updateStyle( 'height', this.getValue() );
													},

													setup : function( selectedTable )
													{
														var val = selectedTable.getStyle( 'width' );
														val && this.setValue( val );
													},
													commit : commitValue
												}
											]
										},
										{
											type : 'html',
											html : '&nbsp;'
										},
										{
											type : 'text',
											id : 'txtCellSpace',
											controlStyle : 'width:3em',
											label : editor.lang.table.cellSpace,
											'default' : 1,
											validate : CKEDITOR.dialog.validate.number( editor.lang.table.invalidCellSpacing ),
											setup : function( selectedTable )
											{
												//set value for txtCellSpace
												var styleString = selectedTable.$.style.cssText.toLowerCase();
												this.setValue(getValueFromStyle(styleString, 'border-spacing') || '');
											},
											commit : function( data, selectedTable )
											{
												//set cell spacing for the table
												if (this.getValue()) {
												    selectedTable.setAttribute('cellSpacing', this.getValue());

												    var styles = this.getDialog().getContentElement( 'advanced', 'advStyles' );
												    styles && styles.updateStyle( 'border-collapse', 'separate' );
												    styles && styles.updateStyle( 'border-spacing', this.getValue() + 'px' );
												} else {
												    selectedTable.removeAttribute('cellspacing');
												}
											}
										},
										{
											type : 'text',
											id : 'txtCellPad',
											controlStyle : 'width:3em',
											label : editor.lang.table.cellPad,
											'default' : 1,
											validate : CKEDITOR.dialog.validate.number( editor.lang.table.invalidCellPadding ),
											setup : function( selectedTable )
											{
												//set value for txtCellPad
												var cellStyle = selectedTable.$.getElementsByTagName('td')[0].style.cssText.toLowerCase();
												if (navigator.userAgent.indexOf("MSIE 8") >= 0 || navigator.userAgent.indexOf("MSIE 7") >= 0) {
												    this.setValue(getValueFromStyle(cellStyle, 'padding-right') || '');
												} else {
												    this.setValue(getValueFromStyle(cellStyle, 'padding') || '');
												}
											},											
											commit : commitValue
										}
									]
								}
							]
						},
						{
							type : 'html',
							align : 'right',
							html : ''
						},
						{
							type : 'vbox',
							padding : 0,
							children :
							[
								{
									type : 'text',
									id : 'txtCaption',
									label : editor.lang.table.caption,
									setup : function( selectedTable )
									{
										this.enable();

										var nodeList = selectedTable.getElementsByTag( 'caption' );
										if ( nodeList.count() > 0 )
										{
											var caption = nodeList.getItem( 0 );
											var firstElementChild = caption.getFirst( CKEDITOR.dom.walker.nodeType( CKEDITOR.NODE_ELEMENT ) );

											if ( firstElementChild && !firstElementChild.equals( caption.getBogus() ) )
											{
												this.disable();
												this.setValue( caption.getText() );
												return;
											}

											caption = CKEDITOR.tools.trim( caption.getText() );
											this.setValue( caption );
										}
									},
									commit : function( data, table )
									{
										if ( !this.isEnabled() )
											return;

										var caption = this.getValue(),
											captionElement = table.getElementsByTag( 'caption' );
										if ( caption )
										{
											if ( captionElement.count() > 0 )
											{
												captionElement = captionElement.getItem( 0 );
												captionElement.setHtml( '' );
											}
											else
											{
												captionElement = new CKEDITOR.dom.element( 'caption', editor.document );
												if ( table.getChildCount() )
													captionElement.insertBefore( table.getFirst() );
												else
													captionElement.appendTo( table );
											}
											captionElement.append( new CKEDITOR.dom.text( caption, editor.document ) );
										}
										else if ( captionElement.count() > 0 )
										{
											for ( var i = captionElement.count() - 1 ; i >= 0 ; i-- )
												captionElement.getItem( i ).remove();
										}
									}
								},
								{
									type : 'text',
									id : 'txtSummary',
									label : editor.lang.table.summary,
									setup : function( selectedTable )
									{
										this.setValue( selectedTable.getAttribute( 'summary' ) || '' );
									},
									commit : function( data, selectedTable )
									{
										if ( this.getValue() )
											selectedTable.setAttribute( 'summary', this.getValue() );
										else
											selectedTable.removeAttribute( 'summary' );
									}
								}
							]
						}
					]
				},
				dialogadvtab && dialogadvtab.createAdvancedTab( editor )
			]
		};
	}

	CKEDITOR.dialog.add( 'table', function( editor )
		{
			return tableDialog( editor, 'table' );
		} );
	CKEDITOR.dialog.add( 'tableProperties', function( editor )
		{
			return tableDialog( editor, 'tableProperties' );
		} );
})();

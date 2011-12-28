function UITable(cols, rows, id, hidden) {
  this.json = {};
  this.id = id;
  this.cols = cols;
  this.rows = rows;
  this.newLine = false;
  if (hidden!=undefined) {
    this.hidden = hidden;
    var glob = document.getElementById(this.hidden);
    glob.style.display = 'none';
    this.json = eval(glob.value);
    if (this.json!=undefined) {
    	this.rows = this.json.length;
    }
  }
}

UITable.prototype.setTypes = function(types) {
	this.types = types;
}

UITable.prototype.enableNewLine = function() {
	this.newLine = true;
}

UITable.prototype.disableNewLine = function() {
	this.newLine = false;
}

UITable.prototype.generateInputs = function() {
  var glob = document.getElementById(this.hidden);
  data = eval(glob.value);
  var inp = document.getElementById(this.id);
  out = '';
  out += '<table class="UIFormGrid">';
  for (i=1 ; i<(this.rows+1) ; i++) {
	out += '<tr>';
    for (j=1 ; j<(this.cols.length+1) ; j++) {
      out += '<td class="FieldLabel"><label for="' + this.id + '-' + i + '-' + j + '">'+this.cols[j-1]+'</td>';
      var s = '';
      if (data!=undefined) {
    	  var obj = data[i-1];
    	  if (obj != undefined) {
    		  s=eval('data[i-1].col'+j);
    	  }
      } 
      if (this.types!=undefined) {
    	  switch (this.types[j-1]) {
    	  case "TEXT":
    		  out += '<td class="FieldComponent"><in'+'put id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update();" type="text" value="'+s+'"></in'+'put></td>';
    		  break;
    	  case "TEXTAREA":
    		  out += '<td class="FieldComponent"><text'+'area id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update();">'+s+'</tex'+'tarea></td>';
    		  break;
    	  }	  
      } else {
        out += '<td class="FieldComponent"><in'+'put id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update();" type="text" value="'+s+'"></in'+'put></td>';
      }
      if (this.newLine) {
    	  out += '</tr><tr>';
      }
    }
    out += '</tr>';
  }
  out += '</table>';

  out += '<div style="float:right;">';
  out += '<img src="/eXoResources/skin/sharedImages/Blank.gif" class="MultiFieldAction Remove16x16Icon" alt="Remove Item" title="Remove Item" onclick="javascript:eXo.ecm.UITable.removeRow();">';  
  out += '<img src="/eXoResources/skin/sharedImages/Blank.gif" class="MultiFieldAction AddNewNodeIcon" alt="Add Item" title="Add Item" onclick="javascript:eXo.ecm.UITable.addRow();">';
  out += '</div>';
  
  inp.innerHTML = out;
  this.update();
};

UITable.prototype.addRow = function() {
	this.rows = this.rows + 1;
	this.generateInputs();
}

UITable.prototype.removeRow = function() {
	if (this.rows>1) {
		this.rows = this.rows - 1;
		delete this.json[this.rows];
		this.generateInputs();
	}
}

UITable.prototype.update = function() {
  var glob = document.getElementById(this.hidden);
  var out = '[';
  for (i=1 ; i<(this.rows+1) ; i++) {
    if (i!=1) out += ',';
    out += '{';
    for (j=1 ; j<(this.cols.length+1) ; j++) {
      var s = this.id+'-'+i+'-'+j;
      var str = '';
      if (document.getElementById(s)!=undefined) {
    	  str = document.getElementById(s).value;
    	  str = str.replace(/\n/gi, "<br/>");
      }
      if (j!=1) out += ',';
      out += '"col'+j+'":"'+str+'"';
    }
    out += '}';
  }
  out += ']';
  this.json = eval(out);
  glob.value = out;
};

UITable.prototype.setJson = function(json) {
	this.json = json;
}

UITable.prototype.generateTable = function() {  
  var outinp = document.getElementById(this.id);
  out = '<table class="UITable">';
  out += '<tr>';
  for (j=1 ; j<(this.cols.length+1) ; j++) {
      out += '<th>'+this.cols[j-1]+'</th>';
  }
  out += '</tr>';
  for (i=1 ; i<(this.json.length+1) ; i++) {
    out += '<tr>';
    for (j=1 ; j<(this.cols.length+1) ; j++) {
      out += '<td>'+this.getValue(i, j)+'</td>';
    }
    out += '</tr>';
  }
  out += '</table>';

  outinp.innerHTML = out;
  
}

UITable.prototype.getValue = function(col, row) {
	var s = eval('this.json[col-1].col'+row);
	return s;
}

/*

HOW TO USE IT IN WCM :
1/ import it in your template :
<script type="text/javascript" src="/eXoStaticResources/table.js"></script>

2/ add the following in your template (dialog or view)
DIALOG EXAMPLE
<tr>                        
  <td colspan="2">
    <%
      String[] fieldDescription = ["jcrPath=/node/exo:description"] ;
      uicomponent.addTextField("description", fieldDescription) ;
    %>
    <div id="wcmtable"></div>
  </td>
  <script type="text/javascript">
    // new UITable ( array of cols, number of rows, div id for inputs, input id where to load/save data )
    eXo.ecm.UITable = new UITable(["Title", "Description"], 3, "wcmtable", "description");
    eXo.ecm.UITable.setTypes(["TEXT", "TEXTAREA"]);
    eXo.ecm.UITable.enableNewLine(); 
    eXo.ecm.UITable.generateInputs();
  </script>
</tr>
      
VIEW EXAMPLE
<div class="summary" >
<%if(node.hasProperty("exo:description")) {%>
  <div id="desctable"></div>
  <script type="text/javascript">
    // new UITable ( array of cols, number of rows, div id for output)
    eXo.ecm.UITable = new UITable(["Title", "Description"], 3, "desctable");
    eXo.ecm.UITable.setJson(<%=node.getProperty("exo:description").getString()%>);
    eXo.ecm.UITable.generateTable();
    //alert(eXo.ecm.UITable.getValue(1, 1));
  </script>
<%}%>
</div>
      

*/
function UITable(cols, rows, id, hidden) {
  if (hidden!=undefined) {
    this.hidden = hidden;
    var glob = document.getElementById(this.hidden);
    glob.style.display = 'none';
  }
  this.id = id;
  this.cols = cols;
  this.rows = rows;
  this.json = {};
  this.newLine = false;
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
      out += '<td class="FieldLabel">'+this.cols[j-1]+'</td>';
      var s = '';
      if (data!=undefined) s=eval('data[i-1].col'+j);
      if (this.types!=undefined) {
    	  switch (this.types[j-1]) {
    	  case "TEXT":
    		  out += '<td class="FieldComponent"><input id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update(this);" type="text" value="'+s+'"></input></td>';
    		  break;
    	  case "TEXTAREA":
    		  out += '<td class="FieldComponent"><textarea id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update(this);">'+s+'</textarea></td>';
    		  break;
    	  }	  
      } else {
        out += '<td class="FieldComponent"><input id="'+this.id+'-'+i+'-'+j+'" onchange="javascript:eXo.ecm.UITable.update(this);" type="text" value="'+s+'"></input></td>';
      }
      if (this.newLine) {
    	  out += '</tr><tr>';
      }
    }
    out += '</tr>';
  }
  out += '</table>';

  inp.innerHTML = out;
};

UITable.prototype.update = function(inp) {
  var glob = document.getElementById(this.hidden);
  glob.value = inp.id + '::' + inp.value;
  var out = '[';
  for (i=1 ; i<(this.rows+1) ; i++) {
    if (i!=1) out += ',';
    out += '{';
    for (j=1 ; j<(this.cols.length+1) ; j++) {
      var s = this.id+'-'+i+'-'+j;
      if (j!=1) out += ',';
      out += '"col'+j+'":"'+document.getElementById(s).value+'"';
    }
    out += '}';
  }
  out += ']';
  glob.value = out;
};

UITable.prototype.setJson = function(json) {
	this.json = json;
}

UITable.prototype.generateTable = function() {  
  var outinp = document.getElementById(this.id);
  out = '<table border=1>';
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
<script type="text/javascript" src="/static/table.js"></script>

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
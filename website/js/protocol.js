var modulePrefix='urn:lsid:broad.mit.edu:cancer.software.genepattern.module.';

function openProtocolWindow(theURL) {
  w = window.open(theURL,'protocol','scrollbars=yes,resizable=yes,width=500,height=350');
  w.focus();
}

//function openGenePatternLink(theURL) {
//  w = window.open(theURL,'genepattern','scrollbars=yes,resizable=yes,width=500,height=350');
//  w.focus();
//}

function openModuleById(id) {
  //alert('opening: '+theURL);
  theURL = '/gp/pages/index.jsf?lsid='+modulePrefix+''+id;
  window.opener.location = theURL;
  window.opener.focus();
}

function openModuleDocById(id) {
  theURL = '/gp/getTaskDoc.jsp?name='+modulePrefix+''+id;
  window.location = theURL;
}





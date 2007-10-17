var lsidPrefix='urn:lsid:broad.mit.edu:cancer.software.genepattern.module.';

//link to a protocal from the splash page
function openProtocolWindow(theURL) {
  w = window.open(theURL,'protocol','scrollbars=yes,resizable=yes,width=500,height=350');
  w.focus();
}

//link to a module from the protocol page
function openModuleById(id) {
  openModuleByLsid(lsidPrefix+''+id);
  window.opener.location = theURL;
  window.opener.focus();
}

function openModuleByLsid(lsid) {
  theURL = '/gp/pages/index.jsf?lsid='+lsid;
  window.opener.location = theURL;
  window.opener.focus();
}

//link to module documentation from the protocol page
function openModuleDocById(id) {
  openModuleDocByLsid(lsidPrefix+''+id);
}

function openModuleDocByLsid(lsid) {
  theURL = '/gp/getTaskDoc.jsp?name='+lsid;
  window.location = theURL;
}








var lsidPrefix='urn:lsid:broad.mit.edu:cancer.software.genepattern.module.';

function openProtocolWindow(theURL) {
  w = window.open(theURL,'protocol','scrollbars=yes,resizable=yes,width=500,height=350');
  w.focus();
}

function openModuleById(theId) {
  openModuleByLsid(lsidPrefix+''+theId);
}

function openModuleByLsid(lsid) {
  theURL = '/gp/pages/index.jsf?lsid='+lsid;
  window.opener.location = theURL;
  window.opener.focus();
}

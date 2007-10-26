//Note on window names
//    'genepattern' - the main GenePattern window
//    'protocol' - the new window which gets created when linking to a protocol from the main GenePattern window

function openProtocolWindow(theURL) {
  if (window.name != 'protocol') {
    window.name = 'genepattern';
  }
  w = window.open(theURL,'protocol','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=500,height=350');
  w.focus();
}

function openModuleByName(theName) {
  theURL = '/gp/pages/index.jsf?lsid='+theName;
  openURL(theURL);
}

function openURL(theURL) {
  w = window.opener;
  if (w != null && w.name != 'protocol') {
    w.location = theURL;
  }
  else {
    w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600');
  }
  w.focus();
  w.select();
}

function setInputFile(cb_name, filename_name, url) {
  w = window.opener;
  w.focus();
  w.document.forms['taskForm'].elements[cb_name].click();
  w.document.forms['taskForm'].elements[filename_name].value = url;
}


//Note on window names
//    'genepattern' - the main GenePattern window
//    'protocol' - the new window which gets created when linking to a protocol from the main GenePattern window

//--------------------
function examplePreprocessDataset() {
  theURL = '/gp/pages/index.jsf?lsid='+'PreprocessDataset';
  w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600');
  w.setTimeout(0);
  w.document.forms['taskForm'].elements['input.filename_cb_url'].click();
  w.document.forms['taskForm'].elements['input.filename_url'].value = 'ftp://ftp.broad.mit.edu/pub/genepattern/datasets/all_aml/all_aml_train.gct';
}
//--------------------


function openProtocolWindow(theURL) {
  if (window.name != 'protocol') {
    window.name = 'genepattern';
  }
  w = window.open(theURL,'protocol','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=500,height=350');
  w.focus();
}

function openModuleWindow(theURL) {
	w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600');
	return w;
}


function openModuleByName(theName) {
  theURL = '/gp/pages/index.jsf?lsid='+theName;
  openURL(theURL);
}

function openModuleByNameParams(theName, key, url) {
  theURL = '/gp/pages/index.jsf?lsid='+theName;
  w = openURL(theURL);
  //setInputFile(key+'_cb_url', key+'_url', url);
  var cb_name = key+'_cb_url';
  var filename_name = key+'_url';
  w.document.forms['taskForm'].elements[cb_name].click();
  w.document.forms['taskForm'].elements[filename_name].value = url;
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
  return w;
}

function setInputFile(cb_name, filename_name, url) {
  w = window.opener;
  w.focus();
  w.document.forms['taskForm'].elements[cb_name].click();
  w.document.forms['taskForm'].elements[filename_name].value = url;
}


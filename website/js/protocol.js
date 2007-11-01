//Note on window names
//    'genepattern' - the main GenePattern window
//    'protocol' - the new window which gets created when linking to a protocol from the main GenePattern window

function openProtocolWindow(theURL) {
  event.returnValue = false; //for IE
  if (window.name != 'protocol') {
    window.name = 'genepattern';
  }
  w = window.open(theURL,'protocol','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=500,height=350');
  w.focus();
}

function openModuleWindow(theURL) {
  event.returnValue = false; //for IE
  w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600');
  w.focus();
  return w;
}

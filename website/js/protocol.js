//Note on window names
//    'genepattern' - the main GenePattern window
//    'protocol' - the new window which gets created when linking to a protocol from the main GenePattern window

function openProtocolWindow(theTarget, theEvent) {
  var theURL = theTarget.href;
  if (theEvent != null) { //for IE
    theEvent.returnValue = false;
  }
  if (window.name != 'protocol') {
    window.name = 'genepattern';
  }
  w = window.open(theURL,'protocol','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=515,height=400',false);
  w.focus();
  return false;
}

function openModuleWindow(theTarget, theEvent) {
  var theURL = theTarget.href;
  if (theEvent != null) { //for IE
    theEvent.returnValue = false;
  }
  w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600',false);
  w.focus();
  return false;
}

function openIgvWindow(theTarget, theEvent) {
    var theURL = theTarget.href;
    if (theEvent != null) { //for IE
      theEvent.returnValue = false;
    }
    w = window.open(theURL,'igv','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600',false);
    w.focus();
    return false;
}

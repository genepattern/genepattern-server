// You can find instructions for this file here:
// http://www.treeview.net

// Decide if the names are links or just the icons
USETEXTLINKS = 1  //replace 0 with 1 for hyperlinks

// Decide if the tree is to start all open or just showing the root folders
STARTALLOPEN = 0 //replace 0 with 1 to show the whole tree

ICONPATH = 'images/' //change if the gif's folder is a subfolder, for example: 'images/'


foldersTree = gFld("edtFTPj/Free", "html/intro.html")

// Getting started
aux1 = insFld(foldersTree, gFld("Getting started", "html/productoverview.html"))
  insDoc(aux1, gLnk("R", "Product Overview", "html/productoverview.html"))
  insDoc(aux1, gLnk("R", "Installation", "html/installation.html"))
  insDoc(aux1, gLnk("R", "Feature List", "html/featurelist.html"))
  insDoc(aux1, gLnk("R", "Key Classes", "html/usingedtftpj.html"))
  insDoc(aux1, gLnk("R", "License Agreement", "html/licenseagreement.html"))
  
// File Transfer Essentials
aux1 = insFld(foldersTree, gFld("File Transfer Essentials", "javascript:parent.op()"))
  // File Transfer Essentials -> FTP Protocol Overview
  aux2 = insFld(aux1, gFld("FTP Protocol Overview", "html/ftpprotocoloverview.html"))
    insDoc(aux2, gLnk("R", "Active and Passive modes", "html/activeandpassivemodes.html"))
    insDoc(aux2, gLnk("R", "FTP Commands", "html/ftpcommands.html"))
    insDoc(aux2, gLnk("R", "Sample Scenarios", "html/samplescenarios.html"))
    insDoc(aux2, gLnk("R", "Data types", "html/datatypes.html"))
    insDoc(aux2, gLnk("R", "Session commands", "html/sessioncommands.html"))
    insDoc(aux2, gLnk("R", "File commands", "html/filecommands.html"))
    insDoc(aux2, gLnk("R", "Directory commands", "html/directorycommands.html"))
    
// How To...
aux1 = insFld(foldersTree, gFld("How To...", "html/howto.html"))
  insDoc(aux1, gLnk("R", "...run the examples", "html/howtorunexamples.html"))
  insDoc(aux1, gLnk("R", "...connect to an FTP server", "html/howtocreateanftpconnection.html"))
  insDoc(aux1, gLnk("R", "...get a directory listing", "html/howtogetadirectorylisting.html"))
  insDoc(aux1, gLnk("R", "...change directories", "html/howtochangedirectories.html"))
  insDoc(aux1, gLnk("R", "...upload, download and delete a file", "html/howtouploadafile.html"))
  insDoc(aux1, gLnk("R", "...use binary or ASCII mode", "html/howtotransfermodes.html"))
  insDoc(aux1, gLnk("R", "...use active or passive mode", "html/howtoconnectmodes.html"))
  insDoc(aux1, gLnk("R", "...transfer using FTP streams", "html/howtotransferstreams.html"))
  insDoc(aux1, gLnk("R", "...monitor transfers and commands", "html/howtomonitortransfers.html"))
  insDoc(aux1, gLnk("R", "...pause and resume transfers", "html/howtopauseresumetransfers.html"))
  insDoc(aux1, gLnk("R", "...use different character encodings", "html/howtousedifferentcharacterencodings.html"))
  insDoc(aux1, gLnk("R", "...FTP through a NAT router/firewall", "html/howtoftpthroughafilewall.html"))
  insDoc(aux1, gLnk("R", "...FTP through a SOCKS proxy", "html/howtoftpthroughasocksproxy.html"))
  insDoc(aux1, gLnk("R", "...FTP through other proxy servers", "html/howtoftpthroughotherproxyservers.html"))
  insDoc(aux1, gLnk("R", "...transfer files securely", "html/howtotransferfilessecurely.html"))
  insDoc(aux1, gLnk("R", "...set up logging", "html/howtosetuplogging.html"))
  insDoc(aux1, gLnk("R", "...diagnose problems", "html/howtodiagnoseproblems.html"))
  insDoc(aux1, gLnk("R", "...get help", "html/support.html"))
 
// Other Documentation
aux1 = insFld(foldersTree, gFld("Other Documentation", "javascript:parent.op()"))  
  insDoc(aux1, gLnk("N", "API Documentation", "../api/index.html"))
  insDoc(aux1, gLnk("R", "RFC 959 - FILE TRANSFER PROTOCOL (FTP)", "rfc/rfc959.txt"))
  insDoc(aux1, gLnk("R", "About this manual", "html/about.html")) 
  
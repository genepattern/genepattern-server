
// Defines main menu for GP 3.0.   Assumes the javascript global variable 'contextRoot' has been defined elsewhere.

function mmLoadMenus() {
  if (window.mm_menu_documentation) return;

  window.mm_menu_tasks = new Menu("root",140,16,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  if(createPublicPipelineAllowed || createPrivatePipelineAllowed) {
  	mm_menu_tasks.addMenuItem("New&nbsp;Pipeline","location=contextRoot + '/pipeline/index.jsf'");
  }

  if(createTaskAllowed) {
  	mm_menu_tasks.addMenuItem("New&nbsp;Module","location=contextRoot + '/modules/creator.jsf'");
  }

  if(createTaskAllowed) {
  	mm_menu_tasks.addMenuItem("Install&nbsp;from&nbsp;repository","location=contextRoot + '/pages/taskCatalog.jsf'");
  }
  
  mm_menu_tasks.addMenuItem("Install&nbsp;from&nbsp;zip","location=contextRoot + '/pages/importTask.jsf'");
  
  mm_menu_tasks.addMenuItem("Manage","location=contextRoot + '/pages/manageTasks.jsf'");
  mm_menu_tasks.hideOnMouseOut=true;
  mm_menu_tasks.bgColor='#CCCC66';
  mm_menu_tasks.menuBorder=0;
  mm_menu_tasks.menuLiteBgColor='#CCCC66';
  mm_menu_tasks.menuBorderBgColor='#CCCC66';

  window.mm_menu_suites = new Menu("root",140,16,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  if(createPublicSuiteAllowed || createPrivateSuiteAllowed) {
  	mm_menu_suites.addMenuItem("New","location=contextRoot + '/pages/createSuite.jsf'");
  	if (createPublicSuiteAllowed && createTaskAllowed){
  		mm_menu_suites.addMenuItem("Install&nbsp;from&nbsp;repository","location=contextRoot + '/pages/suiteCatalog.jsf'");
    }
 	mm_menu_suites.addMenuItem("Install&nbsp;from&nbsp;zip","location=contextRoot + '/pages/importTask.jsf?suite=1'");
    mm_menu_suites.addMenuItem("Manage","location=contextRoot + '/pages/manageSuite.jsf'");
  }

  mm_menu_suites.hideOnMouseOut=true;
  mm_menu_suites.bgColor='#CCCC66';
  mm_menu_suites.menuBorder=0;
  mm_menu_suites.menuLiteBgColor='#CCCC66';
  mm_menu_suites.menuBorderBgColor='#CCCC66';

  window.mm_menu_jobResults = new Menu("root",130,16,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_jobResults.addMenuItem("Results&nbsp;Summary","location=contextRoot + '/jobResults'");
  mm_menu_jobResults.hideOnMouseOut=true;
  mm_menu_jobResults.bgColor='#CCCC66';
  mm_menu_jobResults.menuBorder=0;
  mm_menu_jobResults.menuLiteBgColor='#CCCC66';
  mm_menu_jobResults.menuBorderBgColor='#CCCC66';

  // Help menu
  window.mm_menu_documentation = new Menu("root",153,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_documentation.addMenuItem("Help Forum","window.open('https://www.genepattern.org/help','_blank');");
  mm_menu_documentation.addMenuItem("Tutorial","window.open('https://www.genepattern.org/tutorial/', '_blank');");
  mm_menu_documentation.addMenuItem("Video Tutorial","window.open('https://www.genepattern.org/video-tutorials', '_blank');");
  mm_menu_documentation.addMenuItem("User Guide","window.open('https://www.genepattern.org/user-guide', '_blank');");
  mm_menu_documentation.addMenuItem("Programmers Guide","window.open('https://www.genepattern.org/programmers-guide', '_blank');");
  mm_menu_documentation.addMenuItem("Module&nbsp;Documentation","location=contextRoot + '/getTaskDoc.jsp'");
  mm_menu_documentation.addMenuItem("File Formats","window.open('https://www.genepattern.org/file-formats-guide', '_blank');");
  mm_menu_documentation.addMenuItem("Release&nbsp;Notes","window.open('https://github.com/genepattern/genepattern-server/releases/latest', '_blank')");
  mm_menu_documentation.addMenuItem("FAQ","window.open('https://www.genepattern.org/FAQ', '_blank')");
  mm_menu_documentation.addMenuItem("About","location=contextRoot + '/pages/about.jsf'");
  mm_menu_documentation.hideOnMouseOut=true;
  mm_menu_documentation.bgColor='#CCCC66';
  mm_menu_documentation.menuBorder=0;
  mm_menu_documentation.menuLiteBgColor='#CCCC66';
  mm_menu_documentation.menuBorderBgColor='#CCCC66';

   
  window.mm_menu_resources = new Menu("root",89,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_resources.addMenuItem("Mailing&nbsp;List","window.open('https://www.genepattern.org/contact', '_blank')");
  mm_menu_resources.addMenuItem("Report&nbsp;Bugs","location=contextRoot + '/pages/contactUs.jsf'");
  mm_menu_resources.addMenuItem("Contact Us","location=contextRoot + '/pages/contactUs.jsf'");
 
  mm_menu_resources.hideOnMouseOut=true;
  mm_menu_resources.bgColor='#CCCC66';
  mm_menu_resources.menuBorder=0;
  mm_menu_resources.menuLiteBgColor='#CCCC66';
  mm_menu_resources.menuBorderBgColor='#CCCC66';

  window.mm_menu_downloads = new Menu("root",157,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_downloads.addMenuItem("Programming&nbsp;Libraries","location=contextRoot + '/pages/downloadProgrammingLibaries.jsf'");
  mm_menu_downloads.addMenuItem("Public&nbsp;Datasets","window.open('https://www.genepattern.org/datasets/', '_blank')");
  // mm_menu_downloads.addMenuItem("GParc","window.open('http://www.gparc.org/', '_blank')");
  mm_menu_downloads.hideOnMouseOut=true;
  mm_menu_downloads.bgColor='#CCCC66';
  mm_menu_downloads.menuBorder=0;
  mm_menu_downloads.menuLiteBgColor='#CCCC66';
  mm_menu_downloads.menuBorderBgColor='#CCCC66';

  window.mm_menu_administration = new Menu("root",105,16,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,true);
  mm_menu_administration.addMenuItem("Server&nbsp;Settings","location=contextRoot + '/pages/serverSettings.jsf'");
  mm_menu_administration.hideOnMouseOut=true;
  mm_menu_administration.bgColor='#CCCC66';
  mm_menu_administration.menuBorder=0;
  mm_menu_administration.menuLiteBgColor='#CCCC66';
  mm_menu_administration.menuBorderBgColor='#CCCC66';
  mm_menu_administration.writeMenus();
} // mmLoadMenus()

function MM_reloadPage(init) {  //reloads the window if Nav4 resized
  if (init==true) with (navigator) {if ((appName=="Netscape")&&(parseInt(appVersion)==4)) {
    document.MM_pgW=innerWidth; document.MM_pgH=innerHeight; onresize=MM_reloadPage; }}
  else if (innerWidth!=document.MM_pgW || innerHeight!=document.MM_pgH) location.reload();
}
MM_reloadPage(true);

function MM_preloadImages() { //v3.0
  var d=document; if(d.images){ if(!d.MM_p) d.MM_p=new Array();
    var i,j=d.MM_p.length,a=MM_preloadImages.arguments; for(i=0; i<a.length; i++)
    if (a[i].indexOf("#")!=0){ d.MM_p[j]=new Image; d.MM_p[j++].src=a[i];}}
}

function MM_swapImgRestore() { //v3.0
  var i,x,a=document.MM_sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;
}

function MM_findObj(n, d) { //v4.01
  var p,i,x;  if(!d) d=document; if((p=n.indexOf("?"))>0&&parent.frames.length) {
    d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}
  if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];
  for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=MM_findObj(n,d.layers[i].document);
  if(!x && d.getElementById) x=d.getElementById(n); return x;
}

function MM_swapImage() { //v3.0
  var i,j=0,x,a=MM_swapImage.arguments; document.MM_sr=new Array; for(i=0;i<(a.length-2);i+=3)
   if ((x=MM_findObj(a[i]))!=null){document.MM_sr[j++]=x; if(!x.oSrc) x.oSrc=x.src; x.src=a[i+2];}
}

function MM_jumpMenu(targ,selObj,restore){ //v3.0
  eval(targ+".location='"+selObj.options[selObj.selectedIndex].value+"'");
  if (restore) selObj.selectedIndex=0;
}

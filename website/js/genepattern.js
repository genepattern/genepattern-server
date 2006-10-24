function mmLoadMenus() {
  if (window.mm_menu_0925155042_0) return;
    window.mm_menu_0925155042_0 = new Menu("root",60,18,"Verdana, Arial, Helvetica, sans-serif",10,"#000000","#000099","#EEEEEE","#FFFFFF","left","middle",3,0,500,-5,7,true,true,true,0,true,false);
  mm_menu_0925155042_0.addMenuItem("info","location='info.html'");
  mm_menu_0925155042_0.addMenuItem("reload","location='info.html'");
  mm_menu_0925155042_0.addMenuItem("other","location='info.html'");
   mm_menu_0925155042_0.hideOnMouseOut=true;
   mm_menu_0925155042_0.bgColor='#FFFFFF';
   mm_menu_0925155042_0.menuBorder=1;
   mm_menu_0925155042_0.menuLiteBgColor='#FFFFFF';
   mm_menu_0925155042_0.menuBorderBgColor='#999999';
  window.mm_menu_0925155524_0 = new Menu("root",114,18,"Verdana, Arial, Helvetica, sans-serif",10,"#000000","#000099","#EEEEEE","#FFFFFF","left","middle",3,0,500,-5,7,true,true,true,0,true,false);
  mm_menu_0925155524_0.addMenuItem("modules","location='modules.html'");
  mm_menu_0925155524_0.addMenuItem("create&nbsp;pipeline","location='modules.html'");
  mm_menu_0925155524_0.addMenuItem("info","location='modules.html'");
  mm_menu_0925155524_0.addMenuItem("other","location='modules.html'");
   mm_menu_0925155524_0.hideOnMouseOut=true;
   mm_menu_0925155524_0.bgColor='#FFFFFF';
   mm_menu_0925155524_0.menuBorder=1;
   mm_menu_0925155524_0.menuLiteBgColor='#FFFFFF';
   mm_menu_0925155524_0.menuBorderBgColor='#999999';
   
window.mm_menu_0925165148_0 = new Menu("root",174,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_0925165148_0.addMenuItem("user's&nbsp;manual&nbsp;&&nbsp;tutorial","location='link.html'");
  mm_menu_0925165148_0.addMenuItem("release&nbsp;notes","location='link.html'");
  mm_menu_0925165148_0.addMenuItem("FAQ","location='link.html'");
  mm_menu_0925165148_0.addMenuItem("public&nbsp;datasets","location='link.html'");
  mm_menu_0925165148_0.addMenuItem("task&nbsp;documentation","location='link.html'");
  mm_menu_0925165148_0.addMenuItem("common&nbsp;file&nbsp;formats","location='link.html'");
   mm_menu_0925165148_0.hideOnMouseOut=true;
   mm_menu_0925165148_0.bgColor='#CCCC66';
   mm_menu_0925165148_0.menuBorder=0;
   mm_menu_0925165148_0.menuLiteBgColor='#CCCC66';
   mm_menu_0925165148_0.menuBorderBgColor='#CCCC66';
  window.mm_menu_0925171310_0 = new Menu("root",93,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_0925171310_0.addMenuItem("mailing&nbsp;list","location='link.html'");
  mm_menu_0925171310_0.addMenuItem("report&nbsp;bugs","location='link.html'");
  mm_menu_0925171310_0.addMenuItem("user&nbsp;forum","location='link.html'");
   mm_menu_0925171310_0.hideOnMouseOut=true;
   mm_menu_0925171310_0.bgColor='#CCCC66';
   mm_menu_0925171310_0.menuBorder=0;
   mm_menu_0925171310_0.menuLiteBgColor='#CCCC66';
   mm_menu_0925171310_0.menuBorderBgColor='#CCCC66';
        window.mm_menu_0925171536_0 = new Menu("root",170,18,"Verdana, Arial, Helvetica, sans-serif",10,"#FFFFFF","#9999FF","#333366","#000033","left","middle",3,0,500,-5,7,true,true,true,3,true,false);
  mm_menu_0925171536_0.addMenuItem("install&nbsp;graphical&nbsp;client","location='gclient.html'");
  mm_menu_0925171536_0.addMenuItem("Programming&nbsp;Libraries","location='programming.html'");
   mm_menu_0925171536_0.hideOnMouseOut=true;
   mm_menu_0925171536_0.bgColor='#CCCC66';
   mm_menu_0925171536_0.menuBorder=0;
   mm_menu_0925171536_0.menuLiteBgColor='#CCCC66';
   mm_menu_0925171536_0.menuBorderBgColor='#CCCC66';
mm_menu_0925155524_0.writeMenus();
} // mmLoadMenus()

function MM_reloadPage(init) {  //reloads the window if Nav4 resized
  if (init==true) with (navigator) {if ((appName=="Netscape")&&(parseInt(appVersion)==4)) {
    document.MM_pgW=innerWidth; document.MM_pgH=innerHeight; onresize=MM_reloadPage; }}
  else if (innerWidth!=document.MM_pgW || innerHeight!=document.MM_pgH) location.reload();
}


function MM_preloadImages() { //v3.0
  var d=document; if(d.images){ if(!d.MM_p) d.MM_p=new Array();
    var i,j=d.MM_p.length,a=MM_preloadImages.arguments; for(i=0; i<a.length; i++)
    if (a[i].indexOf("#")!=0){ d.MM_p[j]=new Image; d.MM_p[j++].src=a[i];}}
}





///// Jims functions, not yet approved by Josh
   
// toggleCheckBoxes -- used in combination with a "master" checkbox to toggle
// the state of a collection of child checkboxes.  Assumes the children and parent
// share a common container parent    
function toggleCheckBoxes(maincheckbox, parentId) {	
  var isChecked = maincheckbox.checked;
  var parentElement = document.getElementById(parentId);   
  var elements = parentElement.getElementsByTagName("input");
  for (i = 0; i < elements.length; i++) {	
    if(elements[i].type="checkbox") {		
      elements[i].checked = isChecked;
    }
  }
}
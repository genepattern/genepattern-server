
// directory of where all the images are
var cmThemePanelBase = '/~heng/JSCookMenu/ThemePanel/';

// the follow block allows user to re-define theme base directory
// before it is loaded.
try
{
	if (myThemePanelBase)
	{
		cmThemePanelBase = myThemePanelBase;
	}
}
catch (e)
{
}

var cmThemePanel =
{
  	// main menu display attributes
  	//
  	// Note.  When the menu bar is horizontal,
  	// mainFolderLeft and mainFolderRight are
  	// put in <span></span>.  When the menu
  	// bar is vertical, they would be put in
  	// a separate TD cell.

  	// HTML code to the left of the folder item
  	mainFolderLeft: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',
  	// HTML code to the right of the folder item
  	mainFolderRight: '<img alt="" src="' + cmThemePanelBase + 'arrow.gif">',
	// HTML code to the left of the regular item
	mainItemLeft: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',
	// HTML code to the right of the regular item
	mainItemRight: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',

	// sub menu display attributes

	// HTML code to the left of the folder item
	folderLeft: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',
	// HTML code to the right of the folder item
	folderRight: '<img alt="" src="' + cmThemePanelBase + 'arrow.gif">',
	// HTML code to the left of the regular item
	itemLeft: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',
	// HTML code to the right of the regular item
	itemRight: '<img alt="" src="' + cmThemePanelBase + 'blank.gif">',
	// cell spacing for main menu
	mainSpacing: 0,
	// cell spacing for sub menus
	subSpacing: 0,
	// auto dispear time for submenus in milli-seconds
	delay: 500
};

// for sub menu horizontal split
var cmThemePanelHSplit = [_cmNoClick, '<td colspan="3" style="height: 5px; overflow: hidden"><div class="ThemePanelMenuSplit"></div></td>'];
// for vertical main menu horizontal split
var cmThemePanelMainHSplit = [_cmNoClick, '<td colspan="3" style="height: 5px; overflow: hidden"><div class="ThemePanelMenuSplit"></div></td>'];
// for horizontal main menu vertical split
var cmThemePanelMainVSplit = [_cmNoClick, '|'];

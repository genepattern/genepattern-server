var last_left_nav_tab = null;
var last_left_nav_tab_new = null;

$(document).ready(function() {
    last_left_nav_tab = $("#left-nav").tabs( "option", "active");
});

$(function()
{
    $(".gp_tours").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();

        //create the steps
        var steps = [
            {
                element: "#topband",//"img[src*='GP-logo.gif']",
                intro: '<div class="tour-header"> GenePattern Icon </div>You can click the GenePattern icon to return to this home page at any time.'
            },
            {
                element: '#navband1',
                intro: '<div class="tour-header"> Navigation Bar </div>Access other pages from the navigation bar.'
            },
            {
                element: '#left-nav-modules-tab',
                intro: '<div class="tour-header"> Modules Tab </div> In the Modules tab, search for a module or pipeline by starting to type its name. Drag and drop modules to favorites and view recently run modules.',
                position: 'right'
            },
            {
                element: '#left-nav-jobs-tab',
                intro: '<div class="tour-header"> Jobs Tab </div>  The Jobs tab lists processing and finished jobs with results files.',
                position: 'right'
            },
            {
                element: '#left-nav-files-tab',
                intro: '<div class="tour-header"> Files Tab</div> The Files tab lists uploaded files on the GenePattern server. You can also copy job result files to the Files Tab.',
                position: 'right'
            }];

        if($("#protocols").is(":visible"))
        {
            steps.push({
                element: '#protocols',
                intro: '<div class="tour-header"> Main Display Pane</div> The main display pane displays interactive information including protocols for common GenePattern analyses.',
                position: 'left'
            });
        }

        if($("#submitJob").is(":visible"))
        {
            steps.push({
                element: '#submitJob',
                intro: '<div class="tour-header"> Main Display Pane</div>The main display pane displays interactive information including protocols for common GenePattern analyses.',
                position: 'left'
            });
        }

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End Tour",
            tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //hack to not show the hidden native file upload button
            $("#submitJob").find(".uploadedinputfile").hide();

            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 2)
            {
                $(this).data("last-left-nav-tab", $("#left-nav").tabs( "option", "active"));

                $( "#left-nav" ).tabs( "option", "active", 0 );
            }
            else if(intro._currentStep == 3)
            {
                $( "#left-nav" ).tabs( "option", "active", 1 );
            }
            
            else if(intro._currentStep == 4)
            {
                $( "#left-nav" ).tabs( "option", "active", 2 );
            }

            else if(targetElement.id == "submitJob")
            {
                //hack to not show the hidden native file upload button
               // $("#submitJob").find(".uploadedinputfile").hide();
            }
        });

        intro.onexit(function()
        {
            introTourCleanup();
        });

        intro.oncomplete(function()
        {
            introTourCleanup();
        });

        intro.start();
    });


    $(".gp_new").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();

        //create the steps
        var steps = [

           {
               tooltipClass: 'browserNotification_2106',
               intro:'<p>A browser notification is now be displayed when jobs complete.</p>'
                   + ' <p><em>Note, that you may need to enable notifications in order to see these</em></p>'
                   + ' <img src="../js/images/browser_notification_webtourJune2021.jpg" alt="GenePattern Browser Notification" width="420" />'
           },
           {
                element: '#left-nav-notebook-tab',
                intro:  '<div class="tour-header"> Notebook Tab</div>'
                   + ' <p>The Notebook tab lists GenePattern notebooks available in the GenePattern Notebook Workspace.</p>'
                   + ' <p>Selecting a notebook from this list will open a new browser tab in the GenePattern Notebook Workspace.</p>'
                   + ' <p>If this is your first time in the <a href="https//notebook.genepattern.org" target="_blank">GP Notebook Workspace</a>, you will be presented with an intro tour in that space.</p>'
                   + ' <p><a href="https//notebook.genepattern.org" target="_blank">More information about GenePattern Notebook can be found here.</a></p>',
                position: 'right'
            },
            {
                intro: '<div class="tour-header"> Release Notes </div> For more details about the June 2021 release '
                    + ' please see the <a href="https://github.com/genepattern/genepattern-server/releases/latest" target="_blank">release notes</a>.'
            }];

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End Tour",
            tooltipClass: "tourStyle",
            scrollToElement: false,
            prevLabel: "",
            showBullets: false
        });

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 1)
            {
                $(this).data("last-left-nav-tab", $("#left-nav").tabs( "option", "active"));

                $( "#left-nav" ).tabs( "option", "active", 3 );
            }
        });

        intro.onexit(function()
        {
            introTourCleanup();
            window.location.href = "/gp";
        });

        intro.oncomplete(function()
        {
            window.location.href = "/gp";
        });

        intro.start();
    });
});


function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function introTourCleanup()
{
    //set tab back to original selection
    $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));

    
    $("#left-nav").tabs("refresh");
}

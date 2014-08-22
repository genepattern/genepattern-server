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
                intro: '<div class="tour-header"> Files Tab</div> The Files tab lists uploaded files on the GenePattern server. You can also copy results files to the Files Tab.',
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
            $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));
        });

        intro.oncomplete(function()
        {
            $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));

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
                element: "#user-box-main",
                intro: '<div class="tour-header"> User Settings </div>Manage user settings and access system messages by clicking here.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#quota-box-main",
                intro: '<div class="tour-header"> Disk Usage </div>Disk usage for the Files tab are displayed.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#uploads-dir",
                intro: '<div class="tour-header"> Files Tab </div>There is a new menu option for a directory. First, click on a directory to slide out menu.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: ".file-widget",
                intro: '<div class="tour-header"> Files Tab </div>The slide out menu now has the option <b>Save Directory</b>. This option will save a local copy of the directory.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Pending Jobs </div>There is a new congestion status indicator for pending jobs. Pending jobs are jobs that have been submitted but have not yet started processing. Green indicates low job volume which estimates that the job should enter the processing state soon.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Pending Jobs </div>Yellow indicates medium job volume which estimates that the job may take hours to enter the processing state.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Pending Jobs </div>Red indicates high job volume which estimates that the job may take days to enter the processing state..',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Status Details </div>The Job Status page now provides additional information about a job is now available by clicking the Show Details link.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Status Details </div>Additional job details such as time added to GenePattern, run start time, cpu usage, and max memory used are displayed.',
                position: 'left',
                scrollToElement: true
            },
            {
                intro: '<div class="tour-header"> The End</div> This is the end of the tour. To learn more about what'
                    + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.9.0" target="_blank">release notes</a>.',
                position: 'left',
                scrollToElement: true
            }
           ];

        if($("#quota-space-label").text().indexOf("/") >= 0)
        {
            //replace the second step with item that mentions quota information
            steps[1] = {
                element: "#quota-box-main",
                intro: '<div class="tour-header"> Disk Usage and Quota </div>Disk usage for the Files tab are displayed. Indicator bar turns red when user nears quota. Quota only applies to Files tab content and not to linked GenomeSpace or Jobs Tab.',
                position: 'left',
                scrollToElement: true
            };

        }
            intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End Tour",
            tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 2)
            {
                $( "#left-nav" ).tabs( "option", "active", 2 );
                $("#uploadTree").find("a[name='uploads']").attr("id", "uploads-dir");
                $("#uploads-dir").click();

            }
            else if(intro._currentStep == 3)
            {
                $("#menus-uploads .file-widget-actions").find(".module-listing").last().addClass("tourHighlight");
            }

            else if(intro._currentStep == 4)
            {
                $("#menus-uploads .file-widget-actions").find(".module-listing").last().removeClass("tourHighlight");

                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });

                $("#main-pane").append('<img class="tour_congestion_image" src="../images/congestion_indicator_green.png" alt="Pending job" width="820" height="500" style="border: none;"></img>');
            }
            else if(intro._currentStep == 5)
            {
                $(".tour_congestion_image").remove();
                $("#main-pane").append('<img class="tour_congestion_image" src="../images/congestion_indicator_yellow.png" alt="Pending job" width="820" height="500" style="border: none;"></img>');
            }
            else if(intro._currentStep == 6)
            {
                $(".tour_congestion_image").remove();
                $("#main-pane").append('<img class="tour_congestion_image" src="../images/congestion_indicator_red.png" alt="Pending job" width="820" height="500" style="border: none;"></img>');
            }
            else if(intro._currentStep == 7)
            {
                $(".tour_congestion_image").remove();
                $("#main-pane").append('<img class="tour_congestion_image" src="../images/job_status_details_show.png" alt="Job Detail" width="820" height="300" style="border: none;"></img>');
            }
            else if(intro._currentStep == 8)
            {
                $(".tour_congestion_image").remove();
                $("#main-pane").append('<img class="tour_congestion_image" src="../images/job_status_details_hide.png" alt="Job Details" width="820" height="500" style="border: none;"></img>');
            }
            else if(intro._currentStep == 9)
            {
                newTourCleanup();
            }
        });

        intro.onchange(function(targetElement)
        {
            if(intro._currentStep == 4)
            {
                $(".search-widget").searchslider("hide");
            }
        });

        intro.onexit(function()
        {
            $("#menus-uploads .file-widget-actions").find(".module-listing").last().removeClass("tourHighlight");

            $(".search-widget").searchslider("hide");

            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab_new);

            newTourCleanup();
        });

        intro.oncomplete(function()
        {
            $("#menus-uploads .file-widget-actions").find(".module-listing").last().removeClass("tourHighlight");

            $(".search-widget").searchslider("hide");

            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab_new);

            newTourCleanup()
        });

        last_left_nav_tab_new =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });
});

function newTourCleanup()
{
    $(".tour_congestion_image").remove();
    $("#main-pane").children(".wasVisibleBefore").show();
    $("#main-pane").children(".wasVisibleBefore").removeClass("wasVisibleBefore");
}

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}


function launchTour()
{
    //alert("webtour: " + window.location);
    /*if (RegExp('webtour', 'gi').test(window.location))
     {
     alert("found webtour step 8");
     var introJobRes = introJs();

     //create the steps
     var steps = [
     {
     element: "#jobTable_paginate",
     intro: '<div class="tour-header"> Job Results Summary Page </div> Paginate',
     position: 'right',
     scrollToElement: true
     }
     ];

     introJobRes.setOptions({
     steps: steps,
     showStepNumbers: false,
     skipLabel: "End tour",
     tooltipClass: "tourStyle"
     });

     //while($("#jobTable_paginate".length == 0)){}
     introJobRes.start();
     }*/
}

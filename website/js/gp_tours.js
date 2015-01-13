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
            },
            {
                element: '#left-nav-genomespace-tour-created',
                intro: '<div class="tour-header"> GenomeSpace Tab</div> If you sign-in using your GenomeSpace account, access via tab that appears here.',
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

                //add the genome space tab
                if($("#left-nav-genomespace-tab").length == 0)
                {
                    $("#left-nav-files-tab").after('<li id="left-nav-genomespace-tour-created">' +
                        '<a href="#left-nav-genomespace">GenomeSpace</a> </li>');

                    $("#left-nav-genomespace-tour-created").parent().after('<li id="left-nav-genomespace-tour-created">' +
                        '<a href="#left-nav-genomespace">GenomeSpace</a>' +
                        '<div id="left-nav-genomespace" class="left-nav-tab">' +
                        '<div class="left-nav-top">' +
                        '<button id="left-nav-genomespace-refresh"><span class="glyphicon glyphicon-refresh"></span> Refresh</button>' +
                        '<a href="http://gsui.genomespace.org" target="_blank"><img id="left-nav-genomespace-logo" src="/gp/pages/genomespace/genomespacelogo.png" /></a>' +
                        '</div>' +
                        '</div>'+
                        '</li>');
                    $("#left-nav").tabs("refresh");
                }
            }
            else if(intro._currentStep == 5)
            {
                $( "#left-nav" ).tabs( "option", "active", 3 );
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
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Comments </div>There is a new commenting feature for jobs. Before running a job, you can now attach comments to it.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Comments </div>Comments for a job will be visible on the Job Status page. From there, you or anyone who can view your job can add, edit, or delete a comment.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Tags </div>There is a new tagging feature for jobs. Before running a job, you can now add tags. Tags are a neat way to label your jobs.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Job Tags </div>Tags for a job will be visible on the Job Status page. From there, you can also delete or add additional tags.',
                position: 'left',
                scrollToElement: true
            },
            {
                intro: '<div class="tour-header"> The End</div> This is the end of the tour. To learn more about what'
                    + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.9.1" target="_blank">release notes</a>.',
                position: 'left',
                scrollToElement: true
            }
           ];

        intro.setOptions({
        steps: steps,
        showStepNumbers: false,
        skipLabel: "End Tour",
        tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 0)
            {
                $(".tour_main_image").remove();

                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });

                $("#main-pane").append('<img class="tour_main_image" src="../images/gp_comments_jobsubmit.png" alt="Job Comments Job Submit" width="815" height="540" style="border: none;"></img>');
            }
            else if(intro._currentStep == 1)
            {
                $(".tour_main_image").remove();

                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });

                $("#main-pane").append('<img class="tour_main_image" src="../images/gp_comments_jobstatus.png" alt="Job Comments Job Status" width="850" height="490" style="border: none;"></img>');
            }
            else if(intro._currentStep == 2)
            {
                $(".tour_main_image").remove();
                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });
                $("#main-pane").append('<img class="tour_main_image" src="../images/gp_tags_jobsubmit.png" alt="Job Tags Job Submit" width="820" height="535" style="border: none;"></img>');
            }
            else if(intro._currentStep == 3)
            {
                $(".tour_main_image").remove();
                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });

                $("#main-pane").append('<img class="tour_main_image" src="../images/gp_tags_jobstatus.png" alt="Job Tags Job Status" width="850" height="450" style="border: none;"></img>');
            }
            else if(intro._currentStep == 4)
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
            introTourCleanup();
            newTourCleanup();
        });

        intro.oncomplete(function()
        {
            introTourCleanup();
            newTourCleanup()
        });

        last_left_nav_tab_new =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });
});

function newTourCleanup()
{
    $(".tour_main_image").remove();
    $("#main-pane").children(".wasVisibleBefore").show();
    $("#main-pane").children(".wasVisibleBefore").removeClass("wasVisibleBefore");

    $("#menus-uploads .file-widget-actions").find(".module-listing").last().removeClass("tourHighlight");

    $(".search-widget").searchslider("hide");

    $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab_new);
}

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function introTourCleanup()
{
    //set tab back to original selection
    $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));

    $("#left-nav-genomespace-tour-created").remove();
    $("#left-nav").tabs("refresh");
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

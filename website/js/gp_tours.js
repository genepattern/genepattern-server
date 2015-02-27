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
                if($("#left-nav-genomespace-tour-created").length == 0)
                {
                    $("#left-nav-files-tab").after('<li id="left-nav-genomespace-tour-created">' +
                        '<a href="#left-nav-genomespace">GenomeSpace</a> </li>');

                    $("#left-nav-genomespace-tour-created").parent().after(
                        '<div id="left-nav-genomespace" class="left-nav-tab">' +
                        '<div class="left-nav-top">' +
                        '<button id="left-nav-genomespace-refresh"><span class="glyphicon glyphicon-refresh"></span> Refresh</button>' +
                        '<a href="http://gsui.genomespace.org" target="_blank"><img id="left-nav-genomespace-logo" src="/gp/pages/genomespace/genomespacelogo.png" /></a>' +
                        '</div>' +
                        '</div>');
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
                intro: '<div class="tour-header"> Module Properties </div> Selecting the properties for a module or pipeline will now display the properties information above the job submit form.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Module Properties </div> When viewing properties of a module, the same information is available as before.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#main-pane",
                intro: '<div class="tour-header"> Module Properties </div> When viewing the properties of a pipeline, the information about the individual ' +
                    'steps in a pipeline is still available as well as some additional information about the pipeline.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#job-results",
                intro: '<div class="tour-header"> Job Results Summary Page </div> The Job Results Summary Page now has additional search options.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#jobTableSearch",
                intro: '<div class="tour-header"> Job Results Search </div> Jobs can be searched for based on comments or tags.',
                position: 'left',
                scrollToElement: true
            },
            {
                element: "#jobTableTagColumn",
                intro: '<div class="tour-header"> Job Results Table </div> There is a new Tags column which lists all the tags associated with a job.',
                position: 'left',
                scrollToElement: true
            },
            {
                intro: '<div class="tour-header"> The End</div> This is the end of the tour. To learn more about what'
                    + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.9.2" target="_blank">release notes</a>.',
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
            if(intro._currentStep == 0)
            {
                $("#main-pane").children().each(function()
                {
                    if($(this).is(":visible"))
                    {
                        $(this).addClass("wasVisibleBefore").hide();
                    }
                });

                $("#main-pane").append("<div id='gpTourDiv'/>");
                $("#gpTourDiv").load("/gp/pages/tour/properties_menu_item.html",
                    function()
                    {
                        $(this).find(".uploadedinputfile").hide();
                        $("#otherOptionsMenu").show();
                        $("#properties").focus();
                    }
                );
            }
            if(intro._currentStep == 1)
            {
                $("#gpTourDiv").load("/gp/pages/tour/module_properties_view.html",
                    function()
                    {
                        $(this).find(".uploadedinputfile").hide();
                    }
                );
            }
            else if(intro._currentStep == 2)
            {
                $("#gpTourDiv").load("/gp/pages/tour/pipeline_properties_view.html",
                    function()
                    {
                        $(this).find(".uploadedinputfile").hide();
                    }
                );
            }
            else if(intro._currentStep == 3)
            {
                $("#gpTourDiv").remove();

                $("#main-pane").children(".wasVisibleBefore").show();
                $("#main-pane").children(".wasVisibleBefore").removeClass("wasVisibleBefore");

                loadJobResults(true);
                $("#jobTable").find(".summaryTitle").children("td").last().attr("id", "jobTableTagColumn");
            }

           /* else if(intro._currentStep == 4)
            {
                $("#jobTable").find(".summaryTitle").children("td").last().attr("id", "jobTableTagColumn");
                //$("#jobTable").scrollRight();
                //$("#jobTableSearch").find(".summaryTitle").children("td").last().attr("id", "jobTableSearchTagColumn");
            }*/
        });


        intro.onexit(function()
        {
            introTourCleanup();
            newTourCleanup();
            window.location.href = "/gp";
        });

        intro.oncomplete(function()
        {
            introTourCleanup();
            newTourCleanup();

            window.location.href = "/gp";
        });

        last_left_nav_tab_new =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });
});

function newTourCleanup()
{
    $("#runTaskSettingsDiv").remove();

    $("#main-pane").children(".wasVisibleBefore").show();
    $("#main-pane").children(".wasVisibleBefore").removeClass("wasVisibleBefore");

    $("#jobTable").find(".summaryTitle").children("td").last().removeAttr("id");
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

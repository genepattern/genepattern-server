//declared in jobResults.xhtml
var hasJavascript;
var isPipeline;

$(function(){

    var jobResultsTab = $("#jobResultsTab");

    if(jobResultsTab != undefined && jobResultsTab.length > 0)
    {
        $("#jobResultsTab").empty();
        $("#main-pane").tabs("destroy");
    }

    //f this is a pipeline with a visualizer
    if(isPipeline && hasJavascript && getURLParameter("openVisualizers") == "true")
    {
        jobResultsTab = $("<ul id='jobResultsTab'><li><a href='#jobResults'>Job Results</a></li></ul>");
        $("#main-pane").prepend(jobResultsTab);

        $("#main-pane").tabs();
    }
});
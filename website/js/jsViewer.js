function getTaskVersion(lsid)
{
    var version = -1;
    var index = lsid.lastIndexOf(":");
    if(index == -1)
    {
        console.log("An error occurred while parsing version from LSID: " + lsid);
    }
    else
    {
        var version = lsid.substring(index+1, lsid.length);
    }

    return version;
}

function cleanUpPanels()
{
    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Hide the protocols, run task form & eula, if visible
    $("#protocols").hide();
    var submitJob = $("#submitJob").hide();
    $("#eula-block").hide();
    $("#jobResults").hide();
    $("#infoMessageDiv").hide();
    $("#errorMessageDiv").hide();

    $("#mainViewerPane").remove();
}

function openJavascriptModule(taskName, taskLsid, launchUrl)
{
    cleanUpPanels();

    var mainViewerPane = $("<div/>").attr("id", "mainViewerPane");
    var headerString = taskName;
    var version = getTaskVersion(taskLsid);
    if(version != -1)
    {
        headerString+= " version " + version;
    }

    var infoBar = $("<div/>").attr("id", "jsViewerInfoBar");
    infoBar.append("<label>" + headerString + "</label>");
    infoBar.css("margin-top", "-43px");

    var actionBar = $("<div/>").attr("id", "actionBar");

    var newWindowImage = $("<img src='../images/newWindow.png' width='18' height='18' />");
    newWindowImage.click(function()
    {
        //alert("opening new window");
        /*window.open(window.location, "_blank");
        //window.focus();
        console.log("my focus");
        window.moveTo(0);*/

        /*The code below works
        var divText = document.getElementById("main-pane").outerHTML;
        var myWindow = window.open('', '_blank', 'width=1000,height=800');
        var doc = myWindow.document;
        doc.open();
        doc.write(divText);
        doc.close();*/

        window.open(launchUrl, "_blank");
    });

    actionBar.append(newWindowImage);
    actionBar.css("float", "right");
    actionBar.css("margin-left", "100px");

    infoBar.append(actionBar);

    mainViewerPane.append(infoBar);


    var jsViewerFrame = $("<iframe width='100%' height='500' frameborder='0' scrolling='no'>GenePattern Javascript Visualization</iframe>");
    jsViewerFrame.attr("src", launchUrl);
    var viewerDiv = $("<div/>").attr("id", "jsViewer");
    viewerDiv.append(jsViewerFrame);
    mainViewerPane.append(viewerDiv);

    $("#main-pane").append(mainViewerPane);

    mainLayout.close('west');
}

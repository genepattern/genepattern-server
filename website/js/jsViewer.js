/*$.fn.gpJavascript = function(options) {
 var settings = $.extend({
 // These are the defaults.
 taskName: "",
 taskLsid: "",
 launchUrl: ""
 }, options );

 this.filter( "div" ).append(function() {
 return _openJavascriptModule();
 });

 return this;

 };*/

(function( $ ) {
    $.widget("ui.gpJavascript", {
        options: {
            taskName: "",
            taskLsid: "",
            url: ""
        },
        _create: function() {

            var self = this,
                opt = self.options,
                el = self.element;

            this._openJavascriptModule();
        },
        _setOption: function (key, value) {},
        _getTaskVersion: function()
        {
            var version = -1;
            var index = this.options.taskLsid.lastIndexOf(":");
            if(index == -1)
            {
                console.log("An error occurred while parsing version from LSID: " + this.options.taskLsid);
            }
            else
            {
                version = this.options.taskLsid.substring(index+1, this.options.taskLsid.length);
            }

            return version;
        },
        _openJavascriptModule: function()
        {
            var self = this;
            var mainViewerPane = $("<div/>").attr("id", "mainViewerPane");
            var headerString = self.options.taskName;
            var version = this._getTaskVersion();
            if(version != -1)
            {
                headerString+= " version " + version;
            }

            var infoBar = $("<div class='ui-layout-north'/>").attr("id", "jsViewerInfoBar");
            infoBar.append("<label>" + headerString + "</label>");

            var actionBar = $("<div/>").attr("id", "actionBar");

            var newWindowImage = $("<img id='openJSWin' src='../images/newWindow.png' width='17' height='17' title='Relaunch in a new window'/>");
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

                var newPage = window.open(self.options.url, "_blank");
                newPage.onload = function() { this.document.title = headerString; }
            });

            actionBar.append(newWindowImage);
            //actionBar.css("float", "right");

            infoBar.append(actionBar);

            mainViewerPane.append(infoBar);

            var jsViewerFrame = $("<iframe class='ui-layout-center' width='100%' height='500'  frameborder='0' scrolling='auto'>GenePattern Javascript Visualization</iframe>");
            jsViewerFrame.attr("src", self.options.url);
            var viewerDiv = $("<div/>").attr("id", "jsViewer");
            viewerDiv.append(jsViewerFrame);
            mainViewerPane.append(viewerDiv);

            this.element.append(mainViewerPane);
        },
        destroy: function() {
            this.element.next().remove();
        }
    });
}( jQuery ));



(function( $ ) {
    $.widget("ui.gpJavascript", {
        mainPaneId: "mainJsViewerPane_" + Math.floor(Math.random() * 100000000),
        options: {
            taskName: "",
            taskLsid: "",
            jobId: "",
            url: "",
            onCreateActionBar: null //a callback to add more options to the action bar
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
        _openJavascriptModule: function() {
            var self = this;

            var mainViewerPane = $("<div/>").attr("id", self.mainPaneId);

            var headerString = self.options.taskName;
            var version = self._getTaskVersion();
            if (version != -1) {
                headerString += " version " + version;
            }

            var infoBar = $("<div class='ui-layout-north'/>").attr("id", "jsViewerInfoBar");
            infoBar.append("<label>" + headerString + "</label>").css("margin-right", "7px");


            if(self.options.taskLsid !== undefined && self.options.taskLsid !== null && self.options.taskLsid.length > 0)
            {
                var docLink = "../getTaskDoc.jsp?name=" + self.options.taskLsid;

                infoBar.append($("<a>Documentation</a>").attr("href", docLink).attr("target", "_blank")
                    .css("float", "right").css("font-weight", "normal").css("margin-right", "18px"));
            }

            var actionBar = $("<span/>").attr("id", "actionBar");

            if(self.options.onCreateActionBar !== undefined && typeof self.options.onCreateActionBar === 'function')
            {
                self.options.onCreateActionBar(actionBar);
            }

            var newWindowImage = $("<img id='openJSWin' src='../images/newWindow.png' width='17' height='17' title='Relaunch in a new window'/>");
            /*newWindowImage.click(function()
            {
                //alert("opening new window");
                //window.open(window.location, "_blank");
                 //window.focus();
                 //console.log("my focus");
                 //window.moveTo(0);

                //The code below works
                 //var divText = document.getElementById("main-pane").outerHTML;
                 //var myWindow = window.open('', '_blank', 'width=1000,height=800');
                 //var doc = myWindow.document;
                 //doc.open();
                 //doc.write(divText);
                 //doc.close();

                var newPage = window.open(self.options.url, "_blank");
                newPage.onload = function() { this.document.title = headerString; }
            });*/

           // actionBar.append(newWindowImage);

            infoBar.append(actionBar);

            mainViewerPane.append(infoBar);

            self.element.append(mainViewerPane);

            mainViewerPane.block(
            {
                message: '<h2><img src="../images/spin.gif" /> Loading...</h2>',
                css: {
                    padding:        0,
                    margin:         0,
                    width:          '30%',
                    top:            '40%',
                    left:           '35%',
                    textAlign:      'center',
                    color:          '#000',
                    border:         '2px solid #aaa',
                    backgroundColor: '#fff',
                    cursor:         'wait'
                },
                centerY: false,
                centerX: false,
                overlayCSS:  {
                    backgroundColor: '#000',
                    opacity:         0.1,
                    cursor:          'wait'
                }
            });

            setTimeout(function(){
                var jsViewerFrame = $("<iframe width='100%' height='100%' frameborder='0' scrolling='auto'>GenePattern Javascript Visualization</iframe>");
                jsViewerFrame.attr("src", self.options.url);
                jsViewerFrame.on("load", function(){
                    //remove the blocking UI
                    mainViewerPane.unblock();
                });

                var iframeWrapper = $("<div></div>")
                    .attr("class", "iframe-wrapper")
                    .append(jsViewerFrame);

                mainViewerPane.append(iframeWrapper);
                mainViewerPane.css("position", "");

            }, 2000 );
        },
        destroy: function() {
            var self = this;
            self.element.find("#" + self.mainPaneId).remove();
            $.Widget.prototype.destroy.call(this);
        }
    });
}( jQuery ));



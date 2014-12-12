function openJsViewer(taskName, launchUrl) {
    $.window.prepare({
        dock: 'right',
        minWinLong: 120
    });

    var myWindow = $("#content").window({
        title: taskName,
        url: launchUrl,
        checkBoundary: true,
        bookmarkable: false,
        scrollable: true,
        onMaximize: function () {
            $("#left-nav").hide();
        },
        onMinimize: function () {
            $("#left-nav").show();
        },
        onCascade: function () {
            $("#left-nav").show();
        }
    });
    myWindow.maximize();
}

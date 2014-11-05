$(function() {
    $("#igv_inline").click(function()
    {
        $("#inline_view").empty();

        $(this).after("<div id='inline_view'></div>");

        $("#inline_view").load($(this).attr("load-link"));
    });
});

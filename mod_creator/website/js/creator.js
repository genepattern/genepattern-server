jQuery(document).ready(function() {
  jQuery(".content").show();
  //toggle the component with class msg_body
  jQuery(".heading").click(function()
  {
    jQuery(this).next(".content").slideToggle(340);
  });
});

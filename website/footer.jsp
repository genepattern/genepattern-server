<%--
  ~ Copyright 2013 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

</td></tr>
<tr>
    <td colspan="4" class="footer">
        <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
                <td><a href="<%=request.getContextPath()%>/pages/about.jsf">About GenePattern</a>&nbsp;|&nbsp;<a href="<%=request.getContextPath()%>/pages/contactUs.jsf">Contact Us</a>
                    <br />
                </td>

                <td>
                    <div align="right">&copy;2003-2014&nbsp;<a href="http://www.broadinstitute.org" target="_blank">Broad Institute, MIT</a>
                    </div>
                <td width="10">&nbsp;</td>
                <td width="27"><a href="http://www.broadinstitute.org" target="_blank"><img src="<%=request.getContextPath()%>/images/broad-symbol.gif" alt="Broad Institute" border="0" height="30" width="27" /></a>
                </td>
                </td>
            </tr>

        </table>
    </td>

</tr>
</table>
</div>

<style type="text/css">
    .loginsettings2 {
        font-family: Verdana, Arial, Helvetica, sans-serif;
        font-weight: normal;
        font-size: 10px;
        line-height: 22px;
        color: white;
        text-decoration: none;
        position: absolute;
        right: 30px;
        top: 0px;
        z-index: 40;
        width: auto;
        height: 25px;
        text-align: right;
    }

    .loginsettings2 a {
        /* color: #000099; */
        text-decoration: none;
        font-weight: bold;
    }

    .loginsettings2 a:hover {
        /* color: #6666FF; */
        text-decoration: none;
        font-weight: bold;
    }

    .loginsettings2 a:active {
        /* color: #FFFFFF; */
        text-decoration: none;
        font-weight: bold;
    }
</style>
<!-- top of page login and search items -->
<div id="top-status-box" style="display: none;">
    <div id="user-box-main">
        <div id="user-box" onclick="userBoxClick();">
            <span class="glyphicon glyphicon-user"></span><span class="user-box-arrow">&#x25BC;</span>
            <div id="user-box-name"></div>
        </div>
    </div>
    <div id="quota-box-main">
        <div id="quota-box">
            <div id="quota-space-progressbar">
                <span id="quota-space-label"></span>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    $(document).ready(function() {
        if (userLoggedIn) {
            initStatusBox();
        }
    });
</script>

<!-- Custom Tooltips -->
<ul id="user-menu" style="display: none;">
    <li class="showSystemMessageSpan" style="display: none;"><a href="#" onclick="showSystemMessage();">View System Message</a></li>
    <li><a href="/gp/pages/accountInfo.jsf">My Settings</a></li>
    <li><a href="/gp/logout">Sign Out</a></li>
</ul>
<div id="disk-quota-tooltip" style="display: none;"></div>

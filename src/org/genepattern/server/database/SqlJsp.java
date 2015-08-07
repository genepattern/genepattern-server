package org.genepattern.server.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;

import javax.servlet.jsp.JspWriter;

import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.StringUtils;

public class SqlJsp {
    public static String trimQuery(final String queryIn) {
        if (queryIn==null) {
            return "";
        }
        // trim off trailing CR and LF's, since they will be recorded in the log!
        String query = queryIn.trim();
        while (query.length() > 0 && Character.isWhitespace(query.charAt(query.length() - 1))) {
            query = query.substring(0, query.length() - 1);
        }
        return query;
    }

    public static void formatQuery(final String query, JspWriter out) throws Throwable {
        try {
            AdminDAO ds = new AdminDAO();
            java.util.Date startTime = new java.util.Date();
            if (query.toLowerCase().startsWith("select")) {
                ResultSet rs = ds.executeSQL(query);
                long elapsedMilliseconds = new java.util.Date().getTime() -
                        startTime.getTime();
                out.println("<font size=\"-1\">" + elapsedMilliseconds +
                        " ms.</font><br><br>");
                out.println(StringUtils.htmlEncode(query) + "<br><br>");
                if (rs.next()) {
                    if (rs.isLast()) {
                        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnCount = rsmd.getColumnCount();
                        StringBuffer ret = new StringBuffer();
                        ret.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"lightgrey\" cols=\"2\">\n");
                        for (int i = 1; i <= columnCount; i++) {
                            ret.append("<tr>\n");
                            ret.append("<td valign=\"top\" align=\"right\" class=\"little\">");
                            ret.append(rsmd.getColumnName(i).toLowerCase());
                            ret.append(": </td>\n");
                            boolean isMoney = rsmd.getColumnType(i) == Types.DECIMAL;
                            boolean isEmail = rsmd.getColumnName(i).startsWith("EMAIL");
                            String s = rs.getString(i);
                            if (isMoney) s = moneyFormat.format(rs.getDouble(i));
                            else if (isEmail) s = "<a href=\"mailto:" + rs.getString(i) + "\">" + s + "</a>";
                            else {
                                s = StringUtils.htmlEncode(s);
                                s = StringUtils.replaceAll(s, "\n", "<br>\n");
                            }
                            ret.append("<td valign=\"top\" class=\"little\">");
                            ret.append(s != null ? s : "&nbsp;");
                            ret.append("</td></tr>");
                        }
                        ret.append("</table>");
                        out.println(ret.toString());
                    } else {
                        rs.previous();
                        out.println(SqlJsp.createTable(rs, "No rows returned by this query.<br>", null));
                    }
                    rs.close();
                    rs = null;
                }
            } else {
                int rowsChanged = ds.executeUpdate(query);
                out.println(rowsChanged + " rows changed.<br>");
            }
        } catch (SQLException ex) {
            out.println(ex.getMessage() + "<br>");
        }
    }
    
    public static String createTable(final ResultSet rs, final String noRowsMessage, final String url) throws SQLException {
        /** convert a resultset into an HTML table */
        boolean bHasQuestion = url != null && (url.indexOf("?") != -1);
        if (rs != null && rs.first()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            StringBuffer ret = new StringBuffer();
            ret.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"lightgrey\" class=\"little\">\n");
            ret.append("<tr>\n");
            for (int i = 1; i <= columnCount; i++) {
                ret.append("<td valign=\"top\" align=\"left\"><b>");
                if (url != null) {
                    ret.append("<a href=\"");
                    ret.append(url);
                    ret.append(bHasQuestion ? "&" : "?");
                    ret.append("order=");
                    ret.append(rsmd.getColumnName(i));
                    ret.append("\">");
                }
                ret.append(rsmd.getColumnName(i));
                if (url != null) ret.append("</a>");
                ret.append("</b></td>\n");
            }
            ret.append("</tr>\n");
            NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
            while (true) {
                ret.append("<tr>\n");
                for (int i = 1; i <= columnCount; i++) {
                    boolean isMoney = rsmd.getColumnType(i) == Types.DECIMAL;
                    boolean isEmail = rsmd.getColumnName(i).startsWith("EMAIL");
                    String s = rs.getString(i);
                    if (isMoney) s = moneyFormat.format(rs.getDouble(i));
                    else if (isEmail) s = "<a href=\"mailto:" + rs.getString(i) + "\">" + s + "</a>";
                    else {
                        s = StringUtils.htmlEncode(s);
                        s = StringUtils.replaceAll(s, "\n", "<br>\n");
                    }
                    ret.append("<td valign=\"top\"" + (isMoney ? " align=\"right\"" : "") + " class=\"little\">");
                    ret.append(s != null ? s : "&nbsp;");
                    ret.append("</td>");
                }
                ret.append("</tr>\n");
                if (!rs.next()) break;
            }
            ret.append("</table>\n");
            return ret.toString();
        } else {
            return noRowsMessage;
        }
    } //...createTable
}

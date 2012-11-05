package org.genepattern.server.executor.lsf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Nov 2, 2012
 * Time: 2:16:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class LsfErrorCheckerImpl implements ILsfErrorChecker
{
    LsfErrorStatus errorStatus;

    LsfErrorCheckerImpl(String lsfLogFileName)
    {
	    BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(lsfLogFileName));
            StringBuffer message = new StringBuffer();
            int exitCode = -1;

            String line;
            boolean lsfErrorFound = false;
            while((line = reader.readLine()) != null)
            {
                //do not write any error including and after the PS section
                if(line.startsWith("PS:"))
                {
                    break;
                }

                //if job failed because it ran out of memory or was killed by admin
                if(line.contains("TERM_MEMLIMIT") || line.contains("TERM_OWNER")
                        || lsfErrorFound)
                {
                    message.append(line);
                    message.append("\n");

                    if(line.contains("exit code"))
                    {
                        //parse out exit code
                        int startIndex = line.indexOf("exit code");
                        String exitCodeString = line.substring(startIndex+10, line.length()-1);
                        try
                        {
                            exitCode = Integer.parseInt(exitCodeString);
                        }
                        catch(NumberFormatException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    lsfErrorFound = true;
                }
            }

            errorStatus = new LsfErrorStatus(exitCode, message.toString());
        }
        catch(IOException io)
        {
           io.printStackTrace();
        }
        finally
        {
            if(reader != null)
            {
                try{reader.close();} catch(IOException e){};
            }
        }
    }

    public LsfErrorStatus getStatus()
    {
	    return errorStatus;
    }
}

package demo;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

public class InetInfo {
    public static void printInetInfo(final String urlSpec) throws UnknownHostException, MalformedURLException, SocketException {
        InetAddress addr=InetAddress.getByName(new URL(urlSpec).getHost());
        NetworkInterface ni=NetworkInterface.getByInetAddress(addr);
        printAddr(urlSpec, addr, ni);
    }

    protected static void printAddr(String urlSpec, InetAddress addr, NetworkInterface ni) {
        System.out.println("// "+urlSpec);
        String nicId = ni == null ? "null" :
            "\""+ni.getDisplayName()+"\"";
        System.out.println("\tmock.add(\""+addr.getHostName()+"\", \""+addr.getHostAddress()+"\", "
                +addr.isAnyLocalAddress()+", "
                +addr.isLoopbackAddress()+", "+nicId+");"
        );
    }

    protected static final void usage() {
        System.out.println("Usage: java -cp ./ "+InetInfo.class.getCanonicalName() + "{urlSpec}, ");
        System.out.println("    urlSpec must be a valid url");
    }
    
    public static void main(final String[] args) {
        if (args.length<=0 || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")) {
            usage();
            System.exit(0);
        }
        String urlSpec=args[0];
        try {
            printInetInfo(urlSpec);
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }

}

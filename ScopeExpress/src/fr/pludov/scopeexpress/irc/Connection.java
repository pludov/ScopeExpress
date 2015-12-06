package fr.pludov.scopeexpress.irc;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Jircs: Java IRC Server. This is a simplistic IRC server. It's not designed
 * for production environments; I mainly wrote it to allow me to test out an IRC
 * bot I'm writing (http://jzbot.googlecode.com) when I'm not connected to the
 * internet. Every other dang IRC server takes a minute or so to try and look up
 * my hostname before realizing that I'm not even connected to the internet.
 * Jircs specifically doesn't do that.
 * 
 * @author Alexander Boyd
 * 
 */
public class Connection implements Runnable
{
	final IRCServer ircServer;
    Socket socket;
    String username;
    String hostname;
    String nick;
    String description;
    
    public Connection(IRCServer ircServer, Socket socket)
    {
        this.ircServer = ircServer;
    	this.socket = socket;
    }
    
    public String getRepresentation()
    {
        return nick + "!" + username + "@" + hostname;
    }
    
    protected void sendQuit(String quitMessage)
    {
        synchronized (ircServer.mutex)
        {
            for (String channelName : new ArrayList<String>(ircServer.channelMap.keySet()))
            {
                Channel channel = ircServer.channelMap.get(channelName);
                channel.channelMembers.remove(this);
                channel.send(":" + getRepresentation() + " QUIT :"
                        + quitMessage);
                if (channel.channelMembers.size() == 0)
                    ircServer.channelMap.remove(channel.name);
            }
        }
    }
    
    @Override
    public void run()
    {	        
        try
        {
        	socket.setKeepAlive(true);
            doServer();
        }
        catch (Exception e)
        {
            try
            {
                socket.close();
            }
            catch (Exception e2)
            {
            }
            e.printStackTrace();
        }
        finally
        {
            if (nick != null && ircServer.connectionMap.get(nick) == this)
            {
                sendQuit("Client disconnected");
            }
        }
    }
    
    protected void sendGlobal(String string)
    {
        send(":" + ircServer.globalServerName + " " + string);
    }
    
    LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<String>(
            1000);
    
    private Thread outThread = new Thread()
    {
        public void run()
        {
            try
            {
                OutputStream out = socket.getOutputStream();
                while (true)
                {
                    String s = outQueue.take();
                    s = s.replace("\n", "").replace("\r", "");
                    s = s + "\r\n";
                    out.write(s.getBytes());
                    out.flush();
                }
            }
            catch (Exception e)
            {
                System.out.println("Outqueue died");
                outQueue.clear();
                outQueue = null;
                e.printStackTrace();
                try
                {
                    socket.close();
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                }
            }
        }
    };
    
    private void doServer() throws Exception
    {
        InetSocketAddress address = (InetSocketAddress) socket
                .getRemoteSocketAddress();
        hostname = address.getAddress().getHostAddress();
        System.out.println("Connection from host " + hostname);
        outThread.start();
        InputStream socketIn = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socketIn));
        String line;
        while ((line = reader.readLine()) != null)
        {
            processLine(line);
        }
        
    }
    
    private void processLine(String line) throws Exception
    {
        System.out.println("Processing line from " + nick + ": " + line);
        String prefix = "";
        if (line.startsWith(":"))
        {
            String[] tokens = line.split(" ", 2);
            prefix = tokens[0];
            line = (tokens.length > 1 ? tokens[1] : "");
        }
        String[] tokens1 = line.split(" ", 2);
        String command = tokens1[0];
        line = tokens1.length > 1 ? tokens1[1] : "";
        String[] tokens2 = line.split("(^| )\\:", 2);
        String trailing = null;
        line = tokens2[0];
        if (tokens2.length > 1)
            trailing = tokens2[1];
        ArrayList<String> argumentList = new ArrayList<String>();
        if (!line.equals(""))
            argumentList.addAll(Arrays.asList(line.split(" ")));
        if (trailing != null)
            argumentList.add(trailing);
        String[] arguments = argumentList.toArray(new String[0]);
        /*
         * Now we actually process the command.
         */
        if (command.matches("[0-9][0-9][0-9]"))
            command = "n" + command;
        Command commandObject = null;
        try
        {
            Command.valueOf(command.toLowerCase());
        }
        catch (Exception e)
        {
        }
        if (commandObject == null)
        {
            try
            {
                commandObject = Command.valueOf(command.toUpperCase());
            }
            catch (Exception e)
            {
            }
        }
        if (commandObject == null)
        {
            sendSelfNotice("That command (" + command
                    + ") isnt a supported command at this server.");
            return;
        }
        if (arguments.length < commandObject.getMin()
                || arguments.length > commandObject.getMax())
        {
            sendSelfNotice("Invalid number of arguments for this"
                    + " command, expected not more than "
                    + commandObject.getMax() + " and not less than "
                    + commandObject.getMin() + " but got " + arguments.length
                    + " arguments");
            return;
        }
        commandObject.run(this, prefix, arguments);
    }
    
    /**
     * Sends a notice from the server to the user represented by this
     * connection.
     * 
     * @param string
     *            The text to send as a notice
     */
    void sendSelfNotice(String string)
    {
        send(":" + ircServer.globalServerName + " NOTICE " + nick + " :" + string);
    }
    
    public static String filterAllowedNick(String theNick)
    {
        return theNick.replace(":", "").replace(" ", "").replace("!", "")
                .replace("@", "").replace("#", "");
    }
    
    private String[] padSplit(String line, String regex, int max)
    {
        String[] split = line.split(regex);
        String[] output = new String[max];
        for (int i = 0; i < output.length; i++)
        {
            output[i] = "";
        }
        for (int i = 0; i < split.length; i++)
        {
            output[i] = split[i];
        }
        return output;
    }
    
    public void send(String s)
    {
        Queue<String> testQueue = outQueue;
        if (testQueue != null)
        {
            System.out.println("Sending line to " + nick + ": " + s);
            try {
            	testQueue.add(s);
            } catch(IllegalStateException e) {
            	// La connection est certainement morte...
            	e.printStackTrace();
            }
        }
    }
}

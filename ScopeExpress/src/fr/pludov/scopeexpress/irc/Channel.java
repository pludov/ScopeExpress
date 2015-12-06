package fr.pludov.scopeexpress.irc;

import java.util.ArrayList;

public class Channel
{
	final IRCServer ircServer;
	
    ArrayList<Connection> channelMembers = new ArrayList<Connection>();
    String topic;
    final protected String name;
    
    Channel(IRCServer ircServer, String name)
    {
    	this.ircServer = ircServer;
    	this.name = name;
    }
    
    public void sendNot(Connection not, String toSend)
    {
        synchronized (ircServer.mutex)
        {
            for (Connection con : channelMembers)
            {
                if (con != not)
                    con.send(toSend);
            }
        }
    }
    
    public void send(String toSend)
    {
        sendNot(null, toSend);
    }
    
    public void memberQuit(String nick)
    {
        
    }
}
package fr.pludov.scopeexpress.irc;

import java.util.ArrayList;

public enum Command
{
    NICK(1, 1)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            if (con.nick == null)
                doFirstTimeNick(con, arguments[0]);
            else
                doSelfSwitchNick(con, arguments[0]);
        }
        
        private void doSelfSwitchNick(Connection con, String nick)
        {
            synchronized (con.ircServer.mutex)
            {
                String oldNick = con.nick;
                con.nick = Connection.filterAllowedNick(nick);
                con.ircServer.connectionMap.remove(oldNick);
                con.ircServer.connectionMap.put(con.nick, con);
                con.send(":" + oldNick + "!" + con.username + "@"
                        + con.hostname + " NICK :" + con.nick);
                /*
                 * Now we need to notify all channels that we are on
                 */
                for (Channel c : con.ircServer.channelMap.values())
                {
                    if (c.channelMembers.contains(con))
                        c
                                .sendNot(con, ":" + oldNick + "!"
                                        + con.username + "@" + con.hostname
                                        + " NICK :" + con.nick);
                }
            }
        }
        
        private void doFirstTimeNick(Connection con, String nick)
                throws InterruptedException
        {
            con.nick = Connection.filterAllowedNick(nick);
            synchronized (con.ircServer.mutex)
            {
                con.ircServer.connectionMap.put(con.nick, con);
            }
            /*
             * Now we send the user a welcome message and everything.
             */
        }
        
    },
    USER(1, 4)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            if (con.username != null)
            {
                con.send("NOTICE AUTH :You can't change your user "
                        + "information after you've logged in right now.");
                return;
            }
            con.username = arguments[0];
            String forDescription = arguments.length > 3 ? arguments[3]
                    : "(no description)";
            con.description = forDescription;
            /*
             * Now we'll send the user their initial information.
             */
            con.sendGlobal("001 " + con.nick + " :Welcome to "
                    + con.ircServer.globalServerName + ", a Jircs-powered IRC network.");
            con.sendGlobal("004 " + con.nick + " " + con.ircServer.globalServerName
                    + " Jircs");
            con.sendGlobal("375 " + con.nick + " :- " + con.ircServer.globalServerName
                    + " Message of the Day -");
            con.sendGlobal("372 " + con.nick + " :- Hello. Welcome to "
                    + con.ircServer.globalServerName + ", a Jircs-powered IRC network.");
            con
                    .sendGlobal("372 "
                            + con.nick
                            + " :- See http://code.google.com/p/jwutils/wiki/Jircs "
                            + "for more info on Jircs.");
            con.sendGlobal("376 " + con.nick + " :End of /MOTD command.");
        }
    },
    PING(1, 1)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            con.send(":" + con.ircServer.globalServerName + " PONG " + con.ircServer.globalServerName
                    + " :" + arguments[0]);
        }
    },
    JOIN(1, 2)
    {
        
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            if (arguments.length == 2)
            {
                con.sendSelfNotice("This server does not support "
                        + "channel keys at "
                        + "this time. JOIN will act as if you "
                        + "hadn't specified any keys.");
            }
            String[] channelNames = arguments[0].split(",");
            for (String channelName : channelNames)
            {
                if (!channelName.startsWith("#"))
                {
                    con.sendSelfNotice("This server only allows "
                            + "channel names that "
                            + "start with a # sign.");
                    return;
                }
                if (channelName.contains(" "))
                {
                    con.sendSelfNotice("This server does not allow spaces "
                            + "in channel names.");
                    return;
                }
            }
            for (String channelName : channelNames)
            {
                doJoin(con, channelName);
            }
        }
        
        public void doJoin(Connection con, String channelName)
        {
            if (!channelName.startsWith("#"))
            {
                con
                        .sendSelfNotice("This server only allows channel names that "
                                + "start with a # sign.");
                return;
            }
            if (channelName.contains(" "))
            {
                con
                        .sendSelfNotice("This server does not allow spaces in channel names.");
            }
            synchronized (con.ircServer.mutex)
            {
                Channel channel = con.ircServer.channelMap.get(channelName);
                boolean added = false;
                if (channel == null)
                {
                    added = true;
                    channel = new Channel(con.ircServer, channelName);
                    con.ircServer.channelMap.put(channelName, channel);
                }
                if (channel.channelMembers.contains(con))
                {
                    con.sendSelfNotice("You're already a member of "
                            + channelName);
                    return;
                }
                channel.channelMembers.add(con);
                channel.send(":" + con.getRepresentation() + " JOIN "
                        + channelName);
                if (added)
                    con.sendGlobal("MODE " + channelName + " +nt");
                // This is commented out because channel.send takes care of
                // this for us
                // con.send(":" + con.getRepresentation() + " JOIN "
                // + channelName);
                if (channel.topic != null)
                    con.sendGlobal("332 " + con.nick + " " + channel.name
                            + " :" + channel.topic);
                else
                    con.sendGlobal("331 " + con.nick + " " + channel.name
                            + " :No topic is set");
                for (Connection channelMember : channel.channelMembers)
                {// 353,366
                    con.sendGlobal("353 " + con.nick + " = " + channelName
                            + " :" + channelMember.nick);
                }
                con.sendGlobal("366 " + con.nick + " " + channelName
                        + " :End of /NAMES list");
            }
        }
    },
    WHO(0, 2)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            if (arguments.length > 1)
                con
                        .sendSelfNotice("Filtering by operator only using the WHO "
                                + "command isn't yet supported. WHO will act "
                                + "as if \"o\" has not been specified.");
            String person = "";
            if (arguments.length > 0)
                person = arguments[0];
            synchronized (con.ircServer.mutex)
            {
                Channel channel = con.ircServer.channelMap.get(person);
                if (channel != null)
                {
                    for (Connection channelMember : channel.channelMembers)
                    {
                        con.sendGlobal("352 " + con.nick + " " + person
                                + " " + channelMember.username + " " + channelMember.hostname
                                + " " + con.ircServer.globalServerName + " " + channelMember.nick
                                + " H :0 " + channelMember.description);
                    }
                }
                else
                {
                    con
                            .sendSelfNotice("WHO with something other than a channel "
                                    + "as arguments is not supported right now. "
                                    + "WHO will display an empty list of people.");
                }
            }
            con.send("315 " + con.nick + " " + person
                    + " :End of /WHO list.");
        }
    },
    USERHOST(1, 5)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            ArrayList<String> replies = new ArrayList<String>();
            for (String s : arguments)
            {
                Connection user = con.ircServer.connectionMap.get(s);
                if (user != null)
                    replies.add(user.nick + "=+" + user.username + "@"
                            + user.hostname);
            }
            con.sendGlobal("302 " + con.nick + " :"
                    + con.ircServer.delimited(replies.toArray(new String[0]), " "));
        }
    },
    MODE(0, 2)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            if (arguments.length == 1)
            {
                if (arguments[0].startsWith("#"))
                {
                    con.sendGlobal("324 " + con.nick + " " + arguments[0]
                            + " +nt");
                }
                else
                {
                    con
                            .sendSelfNotice("User mode querying not supported yet.");
                }
            }
            else if (arguments.length == 2
                    && (arguments[1].equals("+b") || arguments[1]
                            .equals("+e")))
            {
                if (arguments[0].startsWith("#"))
                {// 368,349
                    if (arguments[1].equals("+b"))
                    {
                        con.sendGlobal("368 " + con.nick + " "
                                + arguments[0]
                                + " :End of channel ban list");
                    }
                    else
                    {
                        con.sendGlobal("349 " + con.nick + " "
                                + arguments[0]
                                + " :End of channel exception list");
                    }
                }
                else
                {
                    con
                            .sendSelfNotice("User mode setting not supported yet for +b or +e.");
                }
            }
            else
            {
                con.sendSelfNotice("Specific modes not supported yet.");
            }
        }
    },
    PART(1, 2)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            String[] channels = arguments[0].split(",");
            for (String channelName : channels)
            {
                synchronized (con.ircServer.mutex)
                {
                    Channel channel = con.ircServer.channelMap.get(channelName);
                    if (channelName == null)
                        con
                                .sendSelfNotice("You're not a member of the channel "
                                        + channelName
                                        + ", so you can't part it.");
                    else
                    {
                        channel.send(":" + con.getRepresentation()
                                + " PART " + channelName);
                        channel.channelMembers.remove(con);
                        if (channel.channelMembers.size() == 0)
                            con.ircServer.channelMap.remove(channelName);
                    }
                }
            }
        }
    },
    QUIT(1, 1)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            con.sendQuit("Quit: " + arguments[0]);
        }
    },
    PRIVMSG(2, 2)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            String[] recipients = arguments[0].split(",");
            String message = arguments[1];
            for (String recipient : recipients)
            {
                if (recipient.startsWith("#"))
                {
                    Channel channel = con.ircServer.channelMap.get(recipient);
                    if (channel == null)
                        con
                                .sendSelfNotice("No such channel, so can't send "
                                        + "a message to it: " + recipient);
                    else /*if (!channel.channelMembers.contains(con))
                        con.sendSelfNotice("You can't send messages to "
                                + "channels you're not at.");
                    else*/
                        channel.sendNot(con, ":" + con.getRepresentation()
                                + " PRIVMSG " + recipient + " :" + message);
                }
                else
                {
                    Connection recipientConnection = con.ircServer.connectionMap
                            .get(recipient);
                    if (recipientConnection == null)
                        con.sendSelfNotice("The user " + recipient
                                + " is not online.");
                    else
                        recipientConnection.send(":"
                                + con.getRepresentation() + " PRIVMSG "
                                + recipient + " :" + message);
                }
            }
        }
    },
    TOPIC(1, 2)
    {
        @Override
        public void run(Connection con, String prefix, String[] arguments)
                throws Exception
        {
            Channel channel = con.ircServer.channelMap.get(arguments[0]);
            if (channel == null)
            {
                con.sendSelfNotice("No such channel for topic viewing: "
                        + arguments[0]);
                return;
            }
            if (arguments.length == 1)
            {
                /*
                 * The user wants to see the channel topic.
                 */
                if (channel.topic != null)
                    con.sendGlobal("332 " + con.nick + " " + channel.name
                            + " :" + channel.topic);
                else
                    con.sendGlobal("331 " + con.nick + " " + channel.name
                            + " :No topic is set");
            }
            else
            {
                /*
                 * The user wants to set the channel topic.
                 */
                channel.topic = arguments[1];
                channel.sendNot(con, ":" + con.getRepresentation()
                        + " TOPIC " + channel.name + " :" + channel.topic);
            }
        }
    };
    private int minArgumentCount;
    private int maxArgumentCount;
    
    private Command(int min, int max)
    {
        minArgumentCount = min;
        maxArgumentCount = max;
    }
    
    public int getMin()
    {
        return minArgumentCount;
    }
    
    public int getMax()
    {
        return maxArgumentCount;
    }
    
    public abstract void run(Connection con, String prefix,
            String[] arguments) throws Exception;
}
package com.jmpp.server;
 
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
 
@ServerEndpoint(value="/ws/chat")
@Singleton
public class ChatServerEndpoint 
{
	static final String PUBLIC_ROOM = "0713e687-7706-4214-b1b8-c70dadfc639a";
	
    static Set<JMPPSession> userSessions = Collections.synchronizedSet(new HashSet<JMPPSession>());
     
    @OnOpen
    public void onOpen(Session userSession) 
    {
        System.out.println("New request received. Id: " + userSession.getId());
        userSessions.add(new JMPPSession(userSession));
    }
  
    @OnClose
    public void onClose(Session userSession) 
    {
        System.out.println("Connection closed. Id: " + userSession.getId());
        
        JMPPSession jmppSession = JMPPSession.getJMPPSessionBySession(userSessions, userSession);
        
        if(jmppSession != null)
        {
        	this.handleDisconnect(jmppSession);
        	userSessions.remove(jmppSession);
        }
        else
        	System.err.println("Error removing seesion for " + userSession);
    }

    private void handleDisconnect(JMPPSession jmppSession) 
    {
    	try
    	{
	    	System.out.println("User Disconnected: " + jmppSession.getDisplayName());
	        
	        JsonObject json = Json.createObjectBuilder()
	        					.add("type", "user_disconnected")
	        					.add("room_identifier", PUBLIC_ROOM)
	        					.add("user", jmppSession.getDisplayName())
	        					.build();
	        
	        for (JMPPSession userSession : userSessions) 
	        {
	        	// Skip sending the message to the user that is disconnected
	        	if(userSession.equals(jmppSession))
	        		continue;
	        	
	        	Session s = userSession.getSession();
	            s.getAsyncRemote().sendText(json.toString());
	        }
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
	}

	@OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

  
    @OnMessage
    public void onMessage(String message, Session userSession) 
    {
    	 JsonReader jsonReader = Json.createReader(new StringReader(message));
    	 JsonObject object = jsonReader.readObject();
    	 
    	 switch(object.get("type").toString())
    	 {
	    	 case "auth":
	    		 handleAuth(userSession, object);
	    		 break;
	    		 
	    	 case "chat_message":
	    		 handleMessage(userSession, object);
	    		 break;
	    	 default:
	    		 System.out.println("Unknown message");
    	 }        

        jsonReader.close();
    }
    
    private void handleAuth(Session session, JsonObject object)
    {
    	try
    	{
    		String displayName = object.getString("display_name");
    		String identifier = object.getString("identifier");
    		
    		System.out.println("Authorizing: " + displayName + " (" + identifier +")");
    		
    		if(JMPPSession.getJMPPSessionByIdentifier(userSessions, identifier) != null)
    		{
    			throw new Exception("Another user is connected with that identifier");
    		}
    		if(JMPPSession.getJMPPSessionByDisplayName(userSessions, displayName) != null)
    		{
    			System.err.println("Warning: Duplicate user connected");
    		}
    		
    		JMPPSession jmppSession = JMPPSession.getJMPPSessionBySession(userSessions, session);
    		
    		if(jmppSession != null)
    		{
    			jmppSession.setDisplayName(displayName);
    			jmppSession.setIdentifier(identifier);
    			
    			JsonObject response = Json.createObjectBuilder()
    					.add("type", "auth_ok")
    					.build();
    			
    			session.getAsyncRemote().sendText(response.toString());
    			
    			// Handle notifying new users of new connection.
    			JsonObject json = Json.createObjectBuilder()
    					.add("type", "user_connected")
    					.add("room_identifier", PUBLIC_ROOM)
    					.add("user", jmppSession.getDisplayName())
    					.build();
    
			    for (JMPPSession userSession : userSessions) 
			    {
			    	// Skip sending the message to the user that connected
			    	if(userSession.equals(jmppSession))
			    		continue;
			    	
			    	Session s = userSession.getSession();
			        s.getAsyncRemote().sendText(json.toString());
			    }
    		}
    		else
    		{
    			System.err.println("Error authenticating user for invalid session");
    		}
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    private void handleMessage(Session session, JsonObject object)
    {
    	try
    	{
	    	System.out.println("Message Received: " + object);
	    	String message = object.getString("chat_message");
	        
	        JsonObject json = Json.createObjectBuilder()
	        					.add("type", "room_message")
	        					.add("room_identifier", PUBLIC_ROOM)
	        					.add("sender", JMPPSession.getJMPPSessionBySession(userSessions, session).getDisplayName())
	        					.add("chat_message", message)
	        					.build();
	        
	        for (JMPPSession jmppSession : userSessions) 
	        {
	        	Session s = jmppSession.getSession();
	            s.getAsyncRemote().sendText(json.toString());
	        }
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
}
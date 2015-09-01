// Websocket Endpoint url
var endPointURL = "ws://" + window.location.host + window.location.pathname + "ws/chat";
 
var chatClient = null;
 
function connect () {
    chatClient = new WebSocket(endPointURL);
    
    chatClient.onmessage = function (event) {
        var messagesArea = document.getElementById("messages");
        var jsonObj = JSON.parse(event.data);
        console.log(jsonObj);
        
        switch(jsonObj.type)
        {
        	case "auth_ok":
        	  	  $('#authModal').modal('hide');
        		break;
        	case "auth_error":
        		break;
        	case "room_message":
        	case "user_message":
                var message = jsonObj.sender + ": " + jsonObj.chat_message + "\r\n";
                messagesArea.value = messagesArea.value + message;
                messagesArea.scrollTop = messagesArea.scrollHeight;
        		break;
        }        
    };
    
    chatClient.onclose = function (event) {
    	console.log('Connection closed');
    };
}
 
function disconnect () {
    chatClient.close();
}
 
function sendChatMessage() 
{     
    var inputElement = document.getElementById("messageInput");
    var message = inputElement.value.trim();
    if (message !== "") {
        var jsonObj = {"type": "chat_message", "chat_message" : message};
        chatClient.send(JSON.stringify(jsonObj));
        inputElement.value = "";
    }
    inputElement.focus();
}

function sendAuth(displayName, identifier) 
{
    if (displayName !== "") {
        var jsonObj = {"type": "auth", "display_name" : displayName, "identifier" : identifier};
        chatClient.send(JSON.stringify(jsonObj));
    }
}
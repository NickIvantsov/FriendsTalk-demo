package com.example.friendstalk;

public class ChatMessage {
    private String sender;
    private String message;

    public ChatMessage(String sender, String message){
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return this.sender + ": " + this.message;
    }

    @Override
    public int hashCode() {
        return (this.sender + this.message).hashCode();
    }
}

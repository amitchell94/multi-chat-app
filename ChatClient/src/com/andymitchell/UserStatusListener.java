package com.andymitchell;

public interface UserStatusListener {
    public void online(String login);
    public void offline(String login);
}

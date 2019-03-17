package com.andymitchell;

public interface MessageListener {
    void onMessage(String fromLogin, String msgBody);
}

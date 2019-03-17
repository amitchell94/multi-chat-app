package com.andymitchell;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

public class ChatClient {
    private final int serverPort;
    private final String serverName;
    private Socket socket;
    private InputStream serverIn;
    private OutputStream serverOut;
    private BufferedReader bufferedIn;

    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>();
    private ArrayList<MessageListener> messageListeners = new ArrayList<>();

    public ChatClient(String serverName, int serverPort) {
        this.serverPort = serverPort;
        this.serverName = serverName;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 8818);
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("Online: " + login);
            }

            @Override
            public void offline(String login) {
                System.out.println("Offline " + login);
            }
        });

        client.addMessageListener((fromLogin, msgBody) ->
                System.out.println("You got a message from " + fromLogin + "===>" + msgBody));

        if (!client.connect()) {
            System.err.println("Connect failed");
        } else {
            System.out.println("Connect successful");
            if (client.login("guest", "guest")) {
                System.out.println("Login successful");

                client.msg("andy", "Hello World");
            } else {
                System.err.println("Login failed");
            }

            //client.logoff();
        }
    }

    private void msg(String sendTo, String msgBody) throws IOException {
        String cmd = "msg " + sendTo + " " + msgBody + "\n";
        serverOut.write(cmd.getBytes());
    }

    private void logoff() throws IOException {
        serverOut.write("logoff\n".getBytes());
    }


    private boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());

        String response = bufferedIn.readLine();
        System.out.println("Response Line: " + response);

        if ("ok login".equalsIgnoreCase(response)) {
            startMessageReader();
            return true;
        } else {
            return false;
        }
    }

    private void startMessageReader() {
        Thread t = new Thread(() -> readMessageLoop());
        t.start();
    }

    private void readMessageLoop() {
        try {
            String line;
            while ((line = bufferedIn.readLine()) != null) {
                String[] tokens = line.split(" ");
                if (tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if ("online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    } else if ("offline".equalsIgnoreCase(cmd)) {
                        handleOffline(tokens);
                    } else if ("msg".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = line.split(" ",3);
                        handleMessage(tokensMsg);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void handleMessage(String[] tokens) {
        String login = tokens[1];
        String msgBody = tokens[2];

        for (MessageListener listener: messageListeners) {
            listener.onMessage(login,msgBody);
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    private boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }


    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }


    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
}

package com.andymitchell;

import com.sun.deploy.util.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet =  new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
         this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = StringUtils.splitString(line," ");
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("quit".equalsIgnoreCase(line) || "logoff".equalsIgnoreCase(line)) {
                    handleLogOff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream,tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokensMsg = line.split(" ",3);
                    handleMessage(tokensMsg);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handleLeave(tokens);
                }
                else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
    }

    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    private boolean isMemberOfTopic (String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker: workerList) {
            if (isTopic) {
                if(worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "msg " + sendTo + ": " + login + " " + body;
                    worker.send(outMsg);
                }
            } else {
                String outMsg = "msg " + login + " " + body;
                worker.send(outMsg);
            }
        }

    }

    private void handleLogOff() throws IOException {
        server.removeWorker(this);
        ArrayList<ServerWorker> workerList = server.getWorkerList();
        String offlineMsg = "offline " + login;

        for (ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                //let others know we are offline.
                worker.send(offlineMsg);
            }
        }
        clientSocket.close();
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];
            if ((login.equals("guest") && login.equals("guest")) || (login.equals("andy") && login.equals("andy"))) {
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully: " + login);

                String onlineMsg = "online " + login;
                ArrayList<ServerWorker> workerList = server.getWorkerList();

                for (ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin()) && worker.getLogin() != null) {
                        //find other users
                        String currentOnlineUsers = "online " + worker.getLogin();
                        send(currentOnlineUsers);
                    }
                    if (!login.equals(worker.getLogin())) {
                        //let others know we are online.
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write((msg + "\n").getBytes());
        }
    }

    public String getLogin() {
        return login;
    }
}

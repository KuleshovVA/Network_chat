package ru.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection {

    private final Socket soket;
    private final Thread rxThread;
    private final BufferedReader in;
    private final TCPConnectionListener eventListener;
    private final BufferedWriter out;

    public TCPConnection(TCPConnectionListener eventListner, String ipAddr, int port) throws IOException{
        this(eventListner, new Socket(ipAddr, port));
    }

    public TCPConnection(TCPConnectionListener eventListner, Socket socket) throws IOException{
        this.eventListener = eventListner;
        this.soket = socket;
        in = new BufferedReader(new InputStreamReader(soket.getInputStream(), Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(soket.getOutputStream(), Charset.forName("UTF-8")));
        rxThread = new Thread(new Runnable() {
             @Override
            public void run() {
                 try {
                     eventListener.onConnectionReady(TCPConnection.this);
                     while (!rxThread.isInterrupted()){
                         eventListener.onReceiveString(TCPConnection.this, in.readLine());
                     }
                 } catch (IOException e){
                     eventListener.onException(TCPConnection.this, e);
                 } finally {
                     eventListener.onDisconnect(TCPConnection.this);
                 }
            }
        });
        rxThread.start();

    }

    public synchronized void SendStirng(String value){
        try {
            out.write(value  + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }

    public synchronized void disconnect(){
        rxThread.interrupt();
        try {
            soket.close();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + soket.getInetAddress() + ": " + soket.getPort();
    }
}

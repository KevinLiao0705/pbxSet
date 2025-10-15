/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 *
 * @author Kevin
 */
public class WebSocketConn extends WebSocketServer {
    private static List<WebSocket> clients = new CopyOnWriteArrayList<>();
    private static int port = 8843;

    private WebSocketConn() {
        super(new InetSocketAddress(port));
    }

    private static final WebSocketConn wskt = new WebSocketConn();


    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //log.info("一个新客户端打开连接...");
        conn.send("Welcome to link wskt server!");


        //客户端ip
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        //log.info("客户端请求的ip:{}", ip);
        int port = conn.getRemoteSocketAddress().getPort();
        //log.info("客户端的port:{}", port);
        //客户端请求的 websocket path
        String resourceDescriptor = handshake.getResourceDescriptor();
        //log.info("客户端请求的  path:{}", resourceDescriptor);
        clients.add(conn);


    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //log.warn("一个客户端断开websocket连接...");
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        //log.info("客户端请求的ip:{}", ip);
        int port = conn.getRemoteSocketAddress().getPort();
        //log.info("客户端的port:{}", port);
        String resourceDescriptor = conn.getResourceDescriptor();
        //log.info("客户端请求的 path:{}", resourceDescriptor);
        //log.info("code:{}", code);
        //log.info("reason:{}", reason);
        //log.info("remote:{}", remote);
        clients.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //log.info("一个客户端发送了消息....");
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        //log.info("客户端请求的ip:{}", ip);
        int port = conn.getRemoteSocketAddress().getPort();
        //log.info("客户端请求的port:{}", port);
        String resourceDescriptor = conn.getResourceDescriptor();
        //log.info("客户端请求的path:{}", resourceDescriptor);
        //log.info("客户端发送的msg:{}", message);

        handleClientReqMsg(conn, message);

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific
            // websocket
            String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
            //log.info("异常客户端的ip:{}", ip);
            int port = conn.getRemoteSocketAddress().getPort();
            //log.info("异常客户端的port:{}", port);
            String resourceDescriptor = conn.getResourceDescriptor();
            //log.info("异常客户端的path:{}", resourceDescriptor);


        }
    }

    @Override
    public void onStart() {
        System.out.println("wskt Server started!");
        setConnectionLostTimeout(100); //连接丢失超时时间100s

        //启动后定时打印 客户端连接信息
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                printCurrentConns();
            }
        }, 1000, 3000); //延迟1s后,每3秒打印一次
    }

    private static void printCurrentConns() {
        int size = clients.size();
        //log.info("--当前共有{}个websocket连接---", size);
        if (size > 0) {
            for (int i = 0; i < clients.size(); i++) {
                String ip = clients.get(i).getRemoteSocketAddress().getAddress().getHostAddress();
                int port = clients.get(i).getRemoteSocketAddress().getPort();
                String resourceDescriptor = clients.get(i).getResourceDescriptor();
                //log.info("第{}个客户端的ip:{},port:{},path:{}", i + 1, ip, port, resourceDescriptor);
            }
        }
        //log.info("--------------------------");
    }


    public static void startServer() throws UnknownHostException {
        wskt.start();
        //log.info("wsktServer started on port: {}", wskt.getPort());
    }


    private static void handleClientReqMsg(WebSocket conn, String reqStr) {
        // 可以在这里处理req/resp形式的ws请求
        if (conn.getResourceDescriptor().equals("/123")) {
            if (reqStr.equals("aaa")) {
                publishMsgToClient("bbb", conn);
            }
        }
        if (conn.getResourceDescriptor().equals("/234")) {
            if (reqStr.equals("ccc")) {
                publishMsgToClient("ddd", conn);
            }
        }
    }


    public static void publishMsgToClient(String msg, WebSocket targetClient) {
        if (targetClient == null) {
            return;
        }
        String ip = targetClient.getRemoteSocketAddress().getAddress().getHostAddress();
        int port = targetClient.getRemoteSocketAddress().getPort();
        String resourceDescriptor = targetClient.getResourceDescriptor();
        //wskt.broadcast(msg, ListUtil.toList(targetClient));
        Collection<WebSocket> collection = new ArrayList<WebSocket>();
        collection.add(targetClient);
        wskt.broadcast(msg, collection);
        //log.info("server 发布消息:{}给客户端ip:{},port:{},path:{}", msg, ip, port, resourceDescriptor);
    }

    public static void publishMsgToSomeClients(String msg, Collection<WebSocket> clients) {
        if (clients == null || clients.size() == 0) {
            return;
        }
        wskt.broadcast(msg, clients);
        //log.info("server 广播消息:{}", msg);
    }


    /**
     * ws://127.0.0.1:8843/123
     *
     * @param ip   127.0.0.1
     * @param port 8843
     * @param path /123
     * @return WebSocket
     */
    public static WebSocket getOneClient(String ip, int port, String path) {
        if (clients.size() > 0) {
            for (WebSocket client : clients) {
                if (client != null) {
                    int cPort = client.getRemoteSocketAddress().getPort();
                    String cip = client.getRemoteSocketAddress().getAddress().getHostAddress();
                    String cPath = client.getResourceDescriptor();
                    if (ip.equals(cip) && port == cPort && path.equals(cPath)) {
                        return client;
                    }
                }
            }
        }
        return null;
    }

    public static void serverPushClientTest(String msg) {
        if (clients.size() > 0) {
            for (WebSocket client : clients) {
                if (client != null) {
                    String ip = client.getRemoteSocketAddress().getAddress().getHostAddress();
                    int port = client.getRemoteSocketAddress().getPort();
                    String resourceDescriptor = client.getResourceDescriptor();

                    //wskt.broadcast(msg, ListUtil.toList(client));
                    
                    Collection<WebSocket> collection = new ArrayList<WebSocket>();
                    collection.add(client);
                    wskt.broadcast(msg, collection);
                    //wskt.broadcast(msg, clients);
                    
                    
                    //log.info("server 推送消息:{}给客户端ip:{},port:{},path:{}", msg, ip, port, resourceDescriptor);
                }
                
            }
        }
    }

}


package com.rtm516.protocol;

import com.nukkitx.network.VarInts;
import com.nukkitx.network.raknet.RakNetServer;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.v9.Bedrock_v9;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static Timer timer;

    public static void main(String[] args) {
        InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", 19132);
        BedrockServer server = new BedrockServer(bindAddress);

        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCCPP");
        pong.setMotd("Demo");
        pong.setPlayerCount(0);
        pong.setMaximumPlayerCount(20);
        pong.setGameType("Demo");
        pong.setProtocolVersion(Bedrock_v9.V9_CODEC.getProtocolVersion());

        // Start a timer to keep the thread running
        timer = new Timer();
        TimerTask task = new TimerTask() { public void run() { } };
        timer.scheduleAtFixedRate(task, 0L, 1000L);

        server.setHandler(new BedrockServerEventHandler() {
            @Override
            public boolean onConnectionRequest(InetSocketAddress address) {
                return true; // Connection will be accepted
            }

            @Override
            public BedrockPong onQuery(InetSocketAddress address) {
                return pong;
            }

            @Override
            public void onSessionCreation(BedrockServerSession serverSession) {
                serverSession.addDisconnectHandler((reason) -> System.out.println("Disconnected"));
                serverSession.setPacketHandler(new TestPacketHandler());
            }

            @Override
            public void onUnhandledDatagram(ChannelHandlerContext ctx, DatagramPacket packet) {
                ByteBuf content = packet.content();
                byte packetId = 0;
                if (content.isReadable()) {
                    packetId = content.readByte();
                }

                // Manually call the ping
                if (packetId == 2) {
                    try {
                        Class c = Class.forName("com.nukkitx.network.raknet.RakNetServer");
                        Object obj = server.getRakNet();
                        Method method = c.getDeclaredMethod("onUnconnectedPing", ChannelHandlerContext.class, DatagramPacket.class);
                        method.setAccessible(true);
                        method.invoke(obj, ctx, packet);
                    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(packet.content().readableBytes());
                    System.out.println(packetId);
                    System.out.println(packet);
                }
            }
        });

        // Start server up
        server.bind().join();
    }
}

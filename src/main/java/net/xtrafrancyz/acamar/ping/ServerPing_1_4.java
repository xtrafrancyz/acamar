package net.xtrafrancyz.acamar.ping;

import net.xtrafrancyz.acamar.Config.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author xtrafrancyz
 */
public class ServerPing_1_4 {
    private final int timeout;
    private final Server server;
    
    public ServerPing_1_4(int timeout, Server server) {
        this.timeout = timeout;
        this.server = server;
    }
    
    public void fetchData(PingResponse response) throws IOException {
        try (Socket sock = new Socket()) {
            sock.setTcpNoDelay(true);
            sock.setSoTimeout(timeout);
            sock.connect(new InetSocketAddress(server.host, server.port), timeout);
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            DataInputStream in = new DataInputStream(sock.getInputStream());
            out.write(0xFE);
            out.write(0x01);
            
            byte[] data = new byte[512];
            int length = in.read(data, 0, 512);
            if (length < 4 || data[0] != (byte) 255)
                throw new IOException("Bad response from server " + server);
            
            String string = new String(data, "UTF-16LE").substring(3);
            String[] split = string.split(new String(new char[]{0x00}));
            
            response.online = true;
            response.motd = split[3];
            response.onlinePlayers = Integer.parseInt(split[4]);
            response.maxPlayers = Integer.parseInt(split[5]);
        }
    }
}

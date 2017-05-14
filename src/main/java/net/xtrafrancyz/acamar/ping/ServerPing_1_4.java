package net.xtrafrancyz.acamar.ping;

import net.xtrafrancyz.acamar.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author xtrafrancyz
 */
public class ServerPing_1_4 extends ServerPing {
    public ServerPing_1_4(Config.Server server) {
        super(server);
    }
    
    @Override
    public void execute(PingResponse response) throws IOException {
        connectTCP(socket -> {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.write(0xFE);
            out.write(0x01);
            
            byte[] data = new byte[512];
            int length = in.read(data, 0, 512);
            if (length < 4 || data[0] != (byte) 255)
                throw new IOException("Bad response from server " + server);
            
            String string = new String(data, "UTF-16LE").substring(3);
            String[] split = string.split(new String(new char[]{0x00}));
            
            response.motd = split[3];
            response.onlinePlayers = Integer.parseInt(split[4]);
            response.maxPlayers = Integer.parseInt(split[5]);
            response.online = true;
        });
    }
}

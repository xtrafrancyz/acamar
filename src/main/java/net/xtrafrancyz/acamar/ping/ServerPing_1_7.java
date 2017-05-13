package net.xtrafrancyz.acamar.ping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.xtrafrancyz.acamar.Config;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author zh32 <zh32 at zh32.de>
 */
@SuppressWarnings("unused")
public class ServerPing_1_7 {
    private final InetSocketAddress host;
    private final int timeout;
    
    public ServerPing_1_7(int timeout, Config.Server server) {
        this.host = new InetSocketAddress(server.host, server.port);
        this.timeout = timeout;
    }
    
    public int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128)
                break;
        }
        return i;
    }
    
    public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
    
    public void fetchData(PingResponse pingResponse) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(this.timeout);
            socket.connect(host, timeout);
            
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(b);
            handshake.writeByte(0x00); //packet id for handshake
            writeVarInt(handshake, 5); //protocol version
            writeVarInt(handshake, this.host.getHostString().length()); //host length
            handshake.writeBytes(this.host.getHostString()); //host string
            handshake.writeShort(host.getPort()); //port
            writeVarInt(handshake, 1); //state (1 for handshake)
            writeVarInt(dataOutputStream, b.size()); //prepend size
            dataOutputStream.write(b.toByteArray()); //write handshake packet
            dataOutputStream.writeByte(0x01); //size is only 1
            dataOutputStream.writeByte(0x00); //packet id for ping
            
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int size = readVarInt(dataInputStream); //size of packet
            int id = readVarInt(dataInputStream); //packet id
            if (id == -1)
                throw new IOException("Premature end of stream.");
            if (id != 0x00) //we want a status response
                throw new IOException("Invalid packetID");
            int length = readVarInt(dataInputStream); //length of json string
            if (length == -1)
                throw new IOException("Premature end of stream.");
            if (length == 0)
                throw new IOException("Invalid string length.");
            
            byte[] in = new byte[length];
            dataInputStream.readFully(in);  //read json string
            
            JsonObject json = new JsonParser().parse(new String(in)).getAsJsonObject();
            
            JsonObject players = json.getAsJsonObject("players");
            pingResponse.onlinePlayers = players.get("online").getAsInt();
            pingResponse.maxPlayers = players.get("max").getAsInt();
            
            JsonElement description = json.get("description");
            if (description.isJsonObject())
                pingResponse.motd = description.getAsJsonObject().get("text").getAsString();
            else
                pingResponse.motd = description.getAsString();
            
            pingResponse.online = true;
        }
    }
}
package net.xtrafrancyz.acamar.ping;

import net.xtrafrancyz.acamar.Acamar;
import net.xtrafrancyz.acamar.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author xtrafrancyz
 */
public abstract class ServerPing {
    protected final Config.Server server;
    protected final InetSocketAddress address;
    
    protected ServerPing(Config.Server server) {
        this.server = server;
        this.address = new InetSocketAddress(server.host, server.port);
    }
    
    public abstract void execute(PingResponse pingResponse) throws IOException;
    
    protected void connectTCP(SocketWorker worker) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(getTimeout());
            socket.connect(address, getTimeout());
            worker.doWork(socket);
        }
    }
    
    protected int getTimeout() {
        return Acamar.instance().config.timeout;
    }
    
    protected interface SocketWorker {
        void doWork(Socket socket) throws IOException;
    }
}

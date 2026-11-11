package com.cleanroom.remotedesktop.relay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelayServerMain {
    private static final Logger log = LoggerFactory.getLogger(RelayServerMain.class);

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 2112; // Standard clean-room relay port
        boolean tls = true;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--host".equals(args[i]) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--no-tls".equals(args[i])) {
                tls = false;
            }
        }

        RelayServer server = new RelayServer(host, port, tls);
        try {
            server.start();
            log.info("Relay Server running. Press Ctrl+C to terminate.");
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            log.error("Failed to start Relay Server", e);
            System.exit(1);
        }
    }
}


package com.library.ui;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/", WebServer::handleRequest);

        server.start();

        System.out.println("Library Management System running on port " + port);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Library Management System</title>
                    <style>
                        body {
                            font-family: Arial;
                            text-align: center;
                            margin-top: 100px;
                        }
                        h1 {
                            color: #333;
                        }
                        .box {
                            padding: 30px;
                            border: 1px solid #ddd;
                            display: inline-block;
                            border-radius: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h1>Library Management System</h1>
                        <p>Deployment successful!</p>
                        <p>Java Web Service is running on Render.</p>
                    </div>
                </body>
                </html>
                """;

        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, html.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(html.getBytes());
        }
    }
}
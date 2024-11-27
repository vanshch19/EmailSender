package com.example.EmailSender1.Model;

public class EmailConfig {
    private String smtpServer;
    private String port;
    private String username;
    // private String password;
    private boolean authenticationRequired;

    // Constructor, getters, and setters
    public EmailConfig(String smtpServer, String port, String username, boolean authenticationRequired) {
        this.smtpServer = smtpServer;
        this.port = port;
        this.username = username;
        // this.password = password;
        this.authenticationRequired = authenticationRequired;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    // public String getPassword() {
    //     return password;
    // }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    @Override
    public String toString() {
        return "EmailConfig{" +
                "smtpServer='" + smtpServer + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                // ", password='" + (password != null ? "****" : null) + '\'' +
                ", authenticationRequired=" + authenticationRequired +
                '}';
    }
}

package com.example.EmailSender1.Model;

public class EmailDetails {
    private String email;
    private String smtpServer;
    private String port;
    private String username;
    private String password;
    private boolean authenticationRequired;

    public EmailDetails(String email, String smtpServer, String port, String username, String password, boolean authenticationRequired) {
        this.email = email;
        this.smtpServer = smtpServer;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationRequired = authenticationRequired;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    public String toString() {
        return "EmailDetails{" +
                "email='" + email + '\'' +
                ", smtpServer='" + smtpServer + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", authenticationRequired=" + authenticationRequired +
                '}';
    }
}

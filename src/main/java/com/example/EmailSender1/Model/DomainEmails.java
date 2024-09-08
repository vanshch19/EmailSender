package com.example.EmailSender1.Model;


import java.util.List;

public class DomainEmails {
    private String domainName;
    private List<String> emails;
    private String smtpServer;
    private String port;
    private String username;
    private String password;
    private boolean authenticationRequired;

    // Parameterized constructor
    public DomainEmails(String domainName, List<String> emails, String smtpServer, String port, 
                        String username, String password, boolean authenticationRequired) {
        this.domainName = domainName;
        this.emails = emails;
        this.smtpServer = smtpServer;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationRequired = authenticationRequired;
    }


    // Getters and setters
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
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
        return "DomainEmails{" +
                "domainName='" + domainName + '\'' +
                ", emails=" + emails +
                ", smtpServer='" + smtpServer + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", authenticationRequired=" + authenticationRequired +
                '}';
    }
}






























// package com.example.EmailSender1.Model;


// import java.util.List;

// public class DomainEmails {
//     private String domainName;
//     private List<String> emails;

//     // Parameterized constructor
//     public DomainEmails(String domainName, List<String> emails) {
//         this.domainName = domainName;
//         this.emails = emails;
//     }


//     public DomainEmails(List<DomainEmails> domainEmailsList) {
//         //TODO Auto-generated constructor stub
//     }


//     // Getters and setters
//     public String getDomainName() {
//         return domainName;
//     }

//     public void setDomainName(String domainName) {
//         this.domainName = domainName;
//     }

//     public List<String> getEmails() {
//         return emails;
//     }

//     public void setEmails(List<String> emails) {
//         this.emails = emails;
//     }

//     @Override
//     public String toString() {
//         return "DomainEmails{" +
//                 "domainName='" + domainName + '\'' +
//                 ", emails=" + emails +
//                 '}';
//     }
// }




















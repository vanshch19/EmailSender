package com.example.EmailSender1.Service;

import javax.mail.*;
import javax.mail.internet.*;

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class EmailSender {

        public static void sendEmail(String distributionList, String segmentList, String senderEmail,
                                 String senderName, int sendInterval, String timeUnit,
                                 String smtpServer, boolean authenticationRequired, String username,
                                 String password, String port, boolean ssl, String sendMode,
                                 List<String> to, String subject, String messageText, String plainTextMessage) {

        String from = senderEmail; // Use the sender's email from form data
        String host = smtpServer;

        // Set up the SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", authenticationRequired ? "true" : "false");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        if (ssl) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", port);
        }

        // Create a session with the SMTP properties and authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Convert the send interval to milliseconds
        long intervalMillis = convertToMilliseconds(sendInterval, timeUnit);

        Timer timer = new Timer();
        for (int i = 0; i < to.size(); i++) {
            final int index = i;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendEmailInternal(session, from, to.get(index), subject, messageText, plainTextMessage);
                }
            }, index * intervalMillis);
        }
    }

    private static long convertToMilliseconds(int sendInterval, String timeUnit) {
        switch (timeUnit) {
            case "seconds":
                return sendInterval * 1000;
            case "minutes":
                return sendInterval * 60000;
            case "hours":
                return sendInterval * 3600000;
            default:
                return 0;
        }
    }

    private static void sendEmailInternal(Session session, String from, String toEmail,
                                          String subject, String messageText, String plainTextMessage) {

        try {
            // Create a message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);

            // Check if messageText is HTML
            boolean isHtml = messageText != null && 
                (messageText.contains("<html>") || 
                messageText.contains("<title>") ||
                messageText.contains("<body>") ||
                messageText.contains("<table>"));

            // Set the content
            if (isHtml) {
                // Create a multipart message
                Multipart multipart = new MimeMultipart();

                // Create the HTML part
                BodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(messageText, "text/html; charset=UTF-8");
                multipart.addBodyPart(htmlPart);

                // Create the plain text part
                BodyPart plainTextPart = new MimeBodyPart();
                plainTextPart.setText(plainTextMessage);
                multipart.addBodyPart(plainTextPart);

                // Set the multipart message to the email message
                message.setContent(multipart);
            } else {
                message.setText(plainTextMessage);
            }

            // Send the message
            Transport.send(message);
            System.out.println("Sent message successfully to " + toEmail);
        } catch (MessagingException e) {
            // Log or handle the exception as needed
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

}



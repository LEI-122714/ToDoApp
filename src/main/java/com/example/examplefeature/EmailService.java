package com.example.examplefeature;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // ⚙️ Configurações básicas do servidor SMTP
    // (substitui pelos teus dados, ex: Gmail SMTP)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "marianasoftware5@gmail.com";
    private static final String PASSWORD = "kzwl wwjn lwqt hnja";

    public static void sendEmail(String recipient, String subject, String bodyText) {
        // Define propriedades SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        // Cria sessão autenticada
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            // Cria a mensagem
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(bodyText);

            // Envia
            Transport.send(message);
            System.out.println("✅ Email enviado com sucesso para: " + recipient);

        } catch (MessagingException e) {
            throw new RuntimeException("❌ Erro ao enviar email: " + e.getMessage(), e);
        }
    }
}

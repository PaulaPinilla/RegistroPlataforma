package co.edu.unipiloto.registroplataforma;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private static final String CORREO_REMITENTE = "celeste1profe@gmail.com";
    private static final String CONTRASENA_APP = "uyqcxzgemreqrmhc";
    private static final String NOMBRE_REMITENTE = "Universidad"; // <- lo que se ve en vez del correo

    public static void enviar(String correoDestino, String asunto, String cuerpo) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(CORREO_REMITENTE, CONTRASENA_APP);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(CORREO_REMITENTE, NOMBRE_REMITENTE));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino));
            message.setSubject(asunto);
            message.setText(cuerpo);

            Transport.send(message);
            Log.d("EmailSender", "Correo enviado correctamente a " + correoDestino);
        } catch (AddressException | UnsupportedEncodingException e) {
            Log.e("EmailSender", "Error con la dirección de correo", e);
        } catch (MessagingException e) {
            Log.e("EmailSender", "Error enviando correo a " + correoDestino, e);
        }
    }
}
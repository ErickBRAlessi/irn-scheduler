package br.alessi;

import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.exceptions.MailerSendException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;


@ApplicationScoped
public class EmailSenderService {

    @ConfigProperty(name = "mailersend.api-key")
    String apiKey;

    @ConfigProperty(name = "mailersend.from")
    String sendFrom;

    @ConfigProperty(name = "mailersend.send-to")
    String toEmail;


    public void sendEmail(MailDO mailDO) {

        Email email = new Email();
        email.setFrom("IRN Fetcher", sendFrom);

        email.addRecipient(toEmail, toEmail);

        email.setSubject(mailDO.getSubject());
        email.setHtml(mailDO.getContent());

        MailerSend ms = new MailerSend();

        ms.setToken(apiKey);

        try {
            MailerSendResponse response = ms.emails().send(email);
            System.out.println(response.messageId);
        } catch (MailerSendException e) {
            e.printStackTrace();
        }
    }

}

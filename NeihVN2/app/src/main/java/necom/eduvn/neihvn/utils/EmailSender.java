package necom.eduvn.neihvn.utils;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Helper class gửi email qua SMTP.
 * <p>
 * Lưu ý: để hoạt động bạn cần cấu hình địa chỉ email và mật khẩu ứng dụng hợp lệ ở các hằng số bên dưới.
 * Với Gmail, hãy bật xác thực hai bước và tạo app password dành riêng cho ứng dụng.
 */
public final class EmailSender {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    /**
     * TODO: Cập nhật email và mật khẩu ứng dụng trước khi build phát hành.
     */
    private static final String SMTP_USERNAME = "truongzxs@gmail.com";
    private static final String SMTP_PASSWORD = "orjt nbrs tlwo dhmj";

    private EmailSender() {
        // Utility class
    }

    public static void sendEmail(String recipient, String subject, String body) throws MessagingException {
        Session session = Session.getInstance(buildProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private static Properties buildProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        return props;
    }
}


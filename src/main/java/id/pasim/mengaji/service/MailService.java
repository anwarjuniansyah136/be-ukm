package id.pasim.mengaji.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.mail.from:}") String mailFrom
    ) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername == null ? "" : mailUsername.trim();
        this.mailPassword = mailPassword == null ? "" : mailPassword.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    public void sendResetPasswordEmail(String toEmail, String recipientName, String token, int expiresInMinutes) {
        if (mailUsername.isEmpty() || mailPassword.isEmpty()) {
            log.warn("SMTP email belum dikonfigurasi (SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD kosong).");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Layanan email belum dikonfigurasi di server. Pastikan SPRING_MAIL_USERNAME dan SPRING_MAIL_PASSWORD sudah diisi di file .env."
            );
        }

        String fromAddress = mailFrom.isEmpty() ? mailUsername : mailFrom;
        String safeName = (recipientName == null || recipientName.isBlank()) ? "Anggota" : escapeHtml(recipientName);
        String safeToken = escapeHtml(token);

        String htmlBody = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                  <div style="background: #14532d; color: #ffffff; padding: 16px 20px; border-radius: 8px; margin-bottom: 20px;">
                    <h2 style="margin: 0; font-size: 18px;">UKM PASIM Mengaji</h2>
                    <p style="margin: 4px 0 0; font-size: 12px; opacity: 0.9;">Pemulihan &amp; Reset Kata Sandi Akun</p>
                  </div>
                  <p style="color: #1f2937; font-size: 14px; line-height: 1.6;">Assalamu'alaikum <strong>%s</strong>,</p>
                  <p style="color: #374151; font-size: 14px; line-height: 1.6;">
                    Kami menerima permintaan untuk mengatur ulang kata sandi akun UKM PASIM Mengaji Anda.
                    Gunakan kode token berikut pada halaman reset kata sandi:
                  </p>
                  <div style="background: #f0fdf4; border: 2px dashed #16a34a; border-radius: 10px; padding: 16px; text-align: center; margin: 20px 0;">
                    <span style="font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #15803d;">%s</span>
                  </div>
                  <p style="color: #4b5563; font-size: 13px; line-height: 1.6;">
                    Kode ini berlaku selama <strong>%d menit</strong>. Demi keamanan, jangan bagikan kode ini kepada siapa pun.
                  </p>
                  <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #f3f4f6; padding-top: 12px;">
                    Jika Anda tidak merasa meminta reset kata sandi, silakan abaikan email ini.
                  </p>
                </div>
                """.formatted(safeName, safeToken, expiresInMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("[UKM PASIM Mengaji] Kode Reset Kata Sandi Anda");
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email reset kata sandi berhasil dikirim ke {}", toEmail);
        } catch (MessagingException | MailException ex) {
            log.error("Gagal mengirim email reset kata sandi ke {}: {}", toEmail, ex.getMessage(), ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gagal mengirim email ke " + toEmail + ". Periksa kembali Gmail App Password dan koneksi SMTP."
            );
        }
    }

    private String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

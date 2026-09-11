package com.virtu_stock.Service;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import com.fasterxml.jackson.databind.ObjectMapper;

// import com.sendgrid.Method;
// import com.sendgrid.Request;
// import com.sendgrid.SendGrid;
// import com.sendgrid.helpers.mail.Mail;
// import com.sendgrid.helpers.mail.objects.Content;
// import com.sendgrid.helpers.mail.objects.Email;
// import com.sendgrid.helpers.mail.objects.Personalization;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    // @Value("${grid.default.cc}")
    // private String defaultCc;

    // @Value("${grid.api.key}")
    // private String gridApiKey;

    // @Value("${grid.from.mail}")
    // private String fromMail;

    @Value("${mail.default.cc:}")
    private String defaultCC;

    private final JavaMailSender mailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();


    /*
     * Send Grid Mail
     * 
     * @Async
     * public void sendHtmlMail(String to, String subject, String htmlBody) {
     * Email from = new Email(fromMail, "VirtuStock (No Reply)");
     * Email toEmail = new Email(to);
     * Content content = new Content("text/html", htmlBody);
     * Mail mail = new Mail(from, subject, toEmail, content);
     * SendGrid sendGrid = new SendGrid(gridApiKey);
     * Request request = new Request();
     * try {
     * request.setMethod(Method.POST);
     * request.setEndpoint("mail/send");
     * request.setBody(mail.build());
     * sendGrid.api(request);
     * } catch (IOException e) {
     * throw new RuntimeException("Failed to send email", e);
     * }
     * }
     * 
     * @Async
     * public void sendHtmlMailWithCC(String to, String subject, String htmlBody) {
     * Email from = new Email(fromMail);
     * Email toEmail = new Email(to);
     * Content content = new Content("text/html", htmlBody);
     * 
     * Mail mail = new Mail();
     * mail.setFrom(from);
     * mail.setSubject(subject);
     * mail.addContent(content);
     * 
     * Personalization personalization = new Personalization();
     * personalization.addTo(toEmail);
     * 
     * if (defaultCc != null && !defaultCc.isBlank()) {
     * personalization.addCc(new Email(defaultCc));
     * }
     * 
     * mail.addPersonalization(personalization);
     * 
     * SendGrid sendGrid = new SendGrid(gridApiKey);
     * Request request = new Request();
     * 
     * try {
     * request.setMethod(Method.POST);
     * request.setEndpoint("mail/send");
     * request.setBody(mail.build());
     * sendGrid.api(request);
     * } catch (IOException e) {
     * throw new RuntimeException("Failed to send email", e);
     * }
     * }
     */

    public boolean hasMXRecord(String email) {
        try {
            String domain = email.substring(email.indexOf('@') + 1);
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[] { "MX" });
            return attrs != null && attrs.get("MX") != null;
        } catch (NamingException e) {
            return false;
        }
    }

    public void sendHtmlMail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addInline("logo", new ClassPathResource("static/Images/logo/logo-name.png"));
            mailSender.send(mimeMessage);

        } catch (MailSendException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email address: " + to);
        } catch (MailException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to send email to " + to + ". Please try again later.");
        } catch (MessagingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Email formatting error.");
        }
    }

    public void sendHtmlMailWithCC(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addInline("logo", new ClassPathResource("static/Images/logo/logo-name.png"));
            if (defaultCC != null && !defaultCC.isBlank()) {
                String[] ccList = defaultCC.split(",");
                helper.setCc(ccList);
            }
            mailSender.send(mimeMessage);

        } catch (MailSendException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email address: " + to);
        } catch (MailException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to send email to " + to + ". Please try again later.");
        } catch (MessagingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Email formatting error.");
        }
    }

    @Async
    public void sendOTPForRegistration(String to, String otp) {
        String subject = "OTP for Registration";
        String htmlContent = """
                                                <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f9fafc; padding: 30px;">
                  <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 10px;
                              box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 25px; text-align: center;">
                    <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                    <h2 style="color:#1d2939;">Verify Your Email</h2>
                    <p style="color:#555;">To complete your registration, please use the OTP below:</p>
                    <h1 style="color:#007bff; letter-spacing: 3px; margin: 20px 0;">%s</h1>
                    <p style="color:#555;">This OTP is valid for <strong>5 minutes</strong>. Please don’t share it with anyone.</p>
                    <p style="margin-top:30px; color:#999; font-size:12px;">If you didn’t request this, please ignore this email.</p>
                    <p style="color:#999; font-size:12px; margin-top:20px;">&copy; %s VirtuStock. All rights reserved.</p>
                  </div>
                </div>
                                            """
                .formatted(otp, LocalDate.now().getYear());
        sendHtmlMail(to, subject, htmlContent);
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to VirtuStock 🎉";
        String htmlContent = """
                                   <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f9fafc; padding: 30px;">
                  <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 25px; text-align: center;">
                    <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                    <h2 style="color:#1d2939;">Welcome to VirtuStock!</h2>
                    <p style="color:#555;">Hi <b>%s</b>, we're thrilled to have you join our investing community.</p>
                    <p style="color:#555;">You’re all set to explore stock trends, analyze IPOs and make smarter investment decisions.</p>
                    <a href="https://virtustock.in/signin" style="display:inline-block; margin-top:15px; background:#1d2939; color:white; text-decoration:none; padding:10px 25px; border-radius:6px;">Get Started</a>
                    <p style="color:#999; font-size:12px; margin-top:30px;">&copy; %s VirtuStock. All rights reserved.</p>
                  </div>
                </div>
                                """
                .formatted(name.split(" ")[0], LocalDate.now().getYear());
        sendHtmlMailWithCC(to, subject, htmlContent);
    }

    @Async
    public void sendOTPForForgotPassword(String to, String otp) {
        String subject = "Account Recovery - VirtuStock";

        String htmlContent = """
                    <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; padding: 30px;">
                      <div style="max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 10px;
                                  box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 25px; text-align: center;">
                          <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                        <h2 style="color:#1d2939;">Reset Your Password</h2>
                        <p style="color:#555;">We received a request to reset your VirtuStock account password.</p>
                        <p style="color:#555;">Use the following One-Time Password (OTP) to proceed:</p>
                        <h1 style="color:#007bff; letter-spacing: 3px; margin: 20px 0;">%s</h1>
                        <p style="color:#555;">This OTP is valid for <strong>5 minutes</strong>.
                           Please do not share it with anyone.</p>
                        <p style="margin-top:30px; color:#999; font-size:12px;">If you didn’t request a password reset,
                           you can safely ignore this email — your account is secure.</p>
                        <p style="color:#999; font-size:12px; margin-top:20px;">
                            &copy; %s VirtuStock. All rights reserved.
                        </p>
                      </div>
                    </div>
                """
                .formatted(otp, LocalDate.now().getYear());

        sendHtmlMail(to, subject, htmlContent);
    }

    @Async
    public void sendPasswordResetMail(String to, String name) {
        String subject = "Account Secure - Password Reset Successful";
        String htmlContent = """
                                                 <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f9fafc; padding: 30px;">
                  <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 10px;
                              box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 25px; text-align: center;">
                      <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                    <h2 style="color:#1d2939;">Your Password Has Been Reset</h2>
                    <p style="color:#555;">Hi <b>%s</b>, your VirtuStock account password was successfully updated.</p>
                    <p style="color:#555;">If you made this change, you can safely ignore this message.</p>
                    <p style="color:#555;">If you didn’t reset your password, please <a href="https://virtustock.in/forgot-password" style="color:#007bff; text-decoration:none;">reset it again</a> immediately or contact our support team.</p>
                    <a href="https://virtustock.in/signin" style="display:inline-block; margin-top:20px; background:#1d2939; color:white; text-decoration:none; padding:10px 25px; border-radius:6px;">Login to VirtuStock</a>
                    <p style="color:#999; font-size:12px; margin-top:30px;">&copy; %s VirtuStock. All rights reserved.</p>
                  </div>
                </div>

                                                """
                .formatted(name.split(" ")[0], LocalDate.now().getYear());
        sendHtmlMail(to, subject, htmlContent);
    }

    public void sendIpoFetchSummaryEmail(String to, Map<String, Object> res) {
        String subject = "📊 IPO Fetch Summary Report - VirtuStock";
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                        <div style="margin:0; padding:0; background-color:#f9fafc; font-family:Arial, sans-serif;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f9fafc; padding:30px 0; width:100%%;">
                                <tr>
                                    <td align="center" style="padding :0 10px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#ffffff; border-radius:10px; padding:25px; border:1px solid #e5e7eb; max-width: 600px;">
                                            <tr>
                                                <td align="center" style="padding:20px;">
                                                  <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                                                    <h2 style="color:#1d2939; font-size:22px; margin:0 0 10px;">IPO Fetch Summary Report</h2>
                                                    <p style="color:#555; font-size:14px; margin:0 0 20px;">
                                                        Here’s the latest status from your IPO alert fetch operation:
                                                    </p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px; border-collapse:collapse; color:#1d2939 !important;">
                                                        <tr>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;"><b>Total Saved</b></td>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;"><b>Total Exists</b></td>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;"><b>Total Skipped</b></td>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;"><b>Total Errors</b></td>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;"><b>Total IPOs</b></td>
                                                            <td style="padding:8px; border-bottom:1px solid #eee; color:#1d2939 !important;">%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        """
                        .formatted(
                                res.get("Total Saved"),
                                res.get("Total Exists"),
                                res.get("Total Skipped"),
                                res.get("Total Errors"),
                                res.get("Total")));

        sb.append(buildIpoTable(res, "Saved Ipos"));
        sb.append(buildIpoTable(res, "Exists Ipos"));
        sb.append(buildIpoTable(res, "Skipped Ipos"));
        sb.append(buildIpoTable(res, "Errors Ipos"));

        sb.append(
                """
                                                   <tr>
                                                  <td align="center" style="padding:25px 0;">
                                                      <p style="color:#999; font-size:12px; margin:0;">
                                                        &copy; %s VirtuStock. All rights reserved.
                                                      </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </div>
                            """
                        .formatted(LocalDate.now().getYear()));

        sendHtmlMailWithCC(to, subject, sb.toString());
    }

    public void sendAsyncErrorMail(String to, Map<String, Object> error) {

        String title = (String) error.getOrDefault("title", "Unknown Task");
        String errorMessage = (String) error.getOrDefault("message", "Unknown error occurred");
        String details = (String) error.getOrDefault("details", "No additional details available");

        String subject = title + " Failed - VirtuStock";

        String htmlBody = """
                <div style="margin:0; padding:0; background-color:#f9fafc; font-family:Arial, sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                           style="background-color:#f9fafc; padding:30px 0; width:100%%;">
                        <tr>
                            <td align="center" style="padding:0 10px;">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                       style="background:#ffffff; border-radius:10px; padding:25px;
                                       border:1px solid #e5e7eb; max-width:600px;">
                                    <tr>
                                        <td align="center" style="padding:20px;">
                                            <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                                            <h2 style="color:#d92d20; font-size:22px; margin:0 0 10px;">
                                                %s Failed
                                            </h2>
                                            <p style="color:#555; font-size:14px; margin:0 0 20px;">
                                                An unexpected error occurred while executing the task.
                                            </p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td>
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                                   style="font-size:14px; border-collapse:collapse;">
                                                <tr>
                                                    <td style="padding:8px; border-bottom:1px solid #eee;">
                                                        <b>Error Message</b>
                                                    </td>
                                                    <td style="padding:8px; border-bottom:1px solid #eee; color:#d92d20;">
                                                        %s
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding:8px; border-bottom:1px solid #eee;">
                                                        <b>Details</b>
                                                    </td>
                                                    <td style="padding:8px; border-bottom:1px solid #eee;">
                                                        %s
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td align="center" style="padding:25px 0;">
                                            <p style="color:#999; font-size:12px; margin:0;">
                                                &copy; %s VirtuStock. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </div>
                """
                .formatted(
                        title,
                        errorMessage,
                        details,
                        LocalDate.now().getYear());

        sendHtmlMailWithCC(to, subject, htmlBody);
    }

    @SuppressWarnings("unchecked")
    private String buildIpoTable(Map<String, Object> res, String key) {
        Object rawObj = res.get(key);
        if (!(rawObj instanceof List<?> rawList) || rawList.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> ipos = new ArrayList<>();
        for (Object obj : rawList) {
            Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
            ipos.add(map);
        }
        boolean hasReason = ipos.get(0).containsKey("reason");
        boolean hasErrorMessage = ipos.get(0).containsKey("message");
        boolean hasType = ipos.get(0).containsKey("type");
        String headerColor = hasErrorMessage ? "#ffecec" : "#eef2f6";

        StringBuilder sb = new StringBuilder();

        sb.append(
                """
                             <tr>
                                <td style="padding-top:25px;">
                                    <h3 style="color:#1d2939; font-size:18px; margin: 0;">%s</h3>
                                </td>
                            </tr>
                            <tr>
                                 <td>
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                        style="margin-top:10px; font-size:14px; border-collapse:collapse;">
                                        <tr style="background:%s;">
                                            <th align="left" style="width:50%%; padding:10px; color:#1a1a1a; color:#1d2939 !important; ">IPO Name</th>
                        """
                        .formatted(key, headerColor));
        if (hasType) {
            sb.append("""
                    <th style="background-color:#eef2f6; color:#1a1a1a; padding:10px; text-align:left;">Type</th>
                      """);
        }

        if (hasReason) {
            sb.append("""
                    <th style="background-color:#eef2f6; color:#1a1a1a; padding:10px; text-align:left;">Reason</th>
                      """);
        }
        if (hasErrorMessage) {
            sb.append(
                    """
                            <th  style="background-color:#ffecec; color:#1a1a1a; padding:10px; text-align:left;">Message</th>
                             """);
        }

        sb.append("</tr>");

        for (Map<String, Object> ipo : ipos) {
            sb.append("<tr>");
            sb.append("""
                         <td style="padding:10px; border-bottom:1px solid #ddd; color:#1d2939 !important;">%s</td>
                    """.formatted(ipo.get("name")));

            if (hasType) {
                sb.append("""
                         <td style="padding:10px; border-bottom:1px solid #ddd; color:#1d2939 !important;">%s</td>
                        """.formatted(ipo.get("type")));
            }

            if (hasReason) {
                sb.append("""
                             <td style="padding:10px; border-bottom:1px solid #ddd; color:#1d2939 !important;">%s</td>
                        """.formatted(ipo.get("reason")));
            }
            if (hasErrorMessage) {
                sb.append("""
                         <td style="padding:10px; border-bottom:1px solid #ddd; color:#1d2939 !important;">%s</td>
                        """.formatted(ipo.get("message")));
            }

            sb.append("</tr>");
        }

        sb.append("""
                            </table>
                        </td>
                    </tr>
                """);

        return sb.toString();
    }

    @Async
    public void sendQueryEmail(String to, String message) {
        String subject = "New Support Query - VirtuStock";

        String htmlContent = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f9fafc; padding: 30px;">
                  <div style="max-width: 600px; margin: 0 auto; background: white; border-radius: 10px;
                              box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 25px;">

                    <div style="text-align:center;">
                        <img src="https://res.cloudinary.com/ms2o/image/upload/v1768293452/logo-name_hfiexu.png" alt="VirtuStock" style="width:140px; margin-bottom:20px;">
                      <h2 style="color:#1d2939;">New Support Query</h2>
                    </div>



                    <div style="margin-top:15px; padding:15px; background:#f1f5f9; border-radius:6px;">
                      <p style="color:#333; white-space: pre-line;">%s</p>
                    </div>

                    <p style="color:#999; font-size:12px; margin-top:30px; text-align:center;">
                      &copy; %s VirtuStock. All rights reserved.
                    </p>
                  </div>
                </div>
                """
                .formatted(

                        message,
                        LocalDate.now().getYear());

        sendHtmlMailWithCC(
                to,
                subject,
                htmlContent);
    }

}

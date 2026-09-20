package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.admin-url:https://moteros.deveps.dev/admin/}")
    private String urlPanel;

    @Value("${security.password-reset.expiration-minutes:15}")
    private long minutosValidez;

    @Override
    public void enviarCodigoRecuperacion(String para, String nombre, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(remitente, "moter@s");
            helper.setTo(para);
            helper.setSubject("Código para recuperar tu contraseña - moter@s");
            helper.setText(construirCuerpo(nombre, codigo), true);

            mailSender.send(message);
            // Sin la direccion: los registros del servidor no guardan datos personales.
            log.info("Email de recuperación de contraseña enviado");
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error al enviar el email de recuperación: {}", e.getMessage());
            throw new IllegalStateException("No se ha podido enviar el email de recuperación", e);
        }
    }

    @Override
    public void avisarDenuncia(String para, String tipo, String motivo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(remitente, "moter@s");
            helper.setTo(para);
            helper.setSubject("Denuncia nueva por revisar - moter@s");
            helper.setText("""
                    Hay una denuncia nueva en moter@s.

                    Contenido: %s
                    Motivo: %s

                    Revísala en el panel: %s
                    """.formatted(tipo, motivo, urlPanel + "denuncias"), false);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("No se ha podido enviar el aviso de denuncia", e);
        }
    }

    /** Correo en HTML con la identidad de la app: asfalto oscuro y naranja. */
    private String construirCuerpo(String nombre, String codigo) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="dark">
                    <title>Recuperar contraseña</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0E0F11; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="background-color: #0E0F11; padding: 28px 16px;">
                        <tr>
                            <td align="center">
                                <table width="480" cellpadding="0" cellspacing="0" role="presentation" style="width: 100%%; max-width: 480px; background-color: #17191C; border: 1px solid #262A2F; border-radius: 16px; overflow: hidden;">
                                    <tr>
                                        <td style="padding: 26px 30px 22px; border-bottom: 1px solid #262A2F;">
                                            <span style="color: #F4F2EE; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;">moter<span style="color: #FF6A13;">@</span>s</span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 30px;">
                                            <p style="margin: 0 0 6px; color: #FF6A13; font-size: 12px; font-weight: 700; letter-spacing: 1.6px; text-transform: uppercase;">Recuperar contraseña</p>
                                            <h1 style="margin: 0 0 16px; color: #F4F2EE; font-size: 22px; line-height: 1.25;">Hola %s</h1>
                                            <p style="margin: 0; color: #A6A29B; font-size: 15px; line-height: 1.6;">
                                                Has pedido recuperar tu contraseña. Escribe este código en la app para continuar:
                                            </p>
                                            <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="margin: 26px 0;">
                                                <tr>
                                                    <td align="center" style="background-color: #1F2226; border: 1px solid #3A3F46; border-radius: 12px; padding: 20px 10px;">
                                                        <span style="color: #FF6A13; font-size: 34px; font-weight: 700; letter-spacing: 10px;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 0; color: #6F6C66; font-size: 13px; text-align: center;">
                                                El código caduca en %d minutos.
                                            </p>
                                            <p style="margin: 26px 0 0; padding-top: 18px; border-top: 1px solid #262A2F; color: #6F6C66; font-size: 13px; line-height: 1.6;">
                                                Si no has sido tú, ignora este correo: tu contraseña actual sigue siendo válida.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color: #121417; padding: 16px 30px; border-top: 1px solid #262A2F;">
                                            <p style="margin: 0; color: #6F6C66; font-size: 12px;">
                                                moter@s · <a href="https://moteros.deveps.dev/privacidad" style="color: #A6A29B; text-decoration: underline;">Privacidad</a>
                                                · <a href="https://moteros.deveps.dev/terminos" style="color: #A6A29B; text-decoration: underline;">Términos</a>
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(nombre), codigo, minutosValidez);
    }
}

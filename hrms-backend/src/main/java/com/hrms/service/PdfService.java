package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.entity.Payslip;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfService {

    @Value("${app.storage.documents-dir}")
    private String documentsDir;

    public String generatePayslipPdf(Payslip payslip, int periodMonth, int periodYear) throws IOException {
        Employee e = payslip.getEmployee();
        String html = buildPayslipHtml(e, payslip, periodMonth, periodYear);

        Path dir = Path.of(documentsDir, "payslips");
        Files.createDirectories(dir);
        String fileName = "payslip_" + e.getId() + "_" + periodYear + "_" + periodMonth + ".pdf";
        Path outputPath = dir.resolve(fileName);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withUri(outputPath.toUri().toString());
            builder.toStream(os);
            builder.withHtmlContent(html, null);
            builder.run();

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                fos.write(os.toByteArray());
            }
        }

        return outputPath.toString();
    }

    private String buildPayslipHtml(Employee e, Payslip p, int month, int year) {
        String hoursLine = p.getHoursWorked() != null
                ? "<tr><td>Hours Worked</td><td>" + p.getHoursWorked() + "</td></tr>"
                : "";
        String bonusLine = p.getBonusAmount() != null && p.getBonusAmount().signum() > 0
                ? "<tr><td>Bonus</td><td>+" + p.getBonusAmount() + "</td></tr>"
                : "";

        return """
                <html>
                <head><style>
                    body { font-family: Helvetica, Arial, sans-serif; padding: 40px; color: #1f2933; }
                    h1 { font-size: 20px; color: #0f172a; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
                    td, th { padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: left; }
                    .total { font-weight: bold; font-size: 16px; }
                </style></head>
                <body>
                    <h1>Payslip - %02d/%d</h1>
                    <p>%s %s | %s | %s</p>
                    <table>
                        <tr><th>Description</th><th>Amount</th></tr>
                        %s
                        <tr><td>Gross Pay</td><td>%s</td></tr>
                        %s
                        <tr><td>Tax Withholding</td><td>-%s</td></tr>
                        <tr><td>Social Security</td><td>-%s</td></tr>
                        <tr><td>Other Deductions</td><td>-%s</td></tr>
                        <tr class="total"><td>Net Pay</td><td>%s</td></tr>
                    </table>
                </body>
                </html>
                """.formatted(
                month, year,
                e.getFirstName(), e.getLastName(), e.getJobTitle(), e.getPayType(),
                hoursLine,
                p.getGrossSalary(),
                bonusLine,
                p.getTaxDeduction(), p.getSocialSecurityDeduction(), p.getOtherDeductions(), p.getNetSalary()
        );
    }
}

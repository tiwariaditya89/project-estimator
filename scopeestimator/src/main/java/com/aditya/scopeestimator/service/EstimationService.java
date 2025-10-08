package com.aditya.scopeestimator.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EstimationService {

    private final ChatModel chatModel;

    @Autowired
    public EstimationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateEstimate(String scopeText, String feedback) {
        String promptText = """
                You are a senior software project estimator and planner. Given the provided Scope of Work (SOW), produce a detailed, professional estimation document suitable for sharing with clients or stakeholders.
                
                Requirements:
                - Output must be in Markdown and presentation-ready.
                - Include these sections: Work Breakdown Structure (WBS), Cost Estimate, Resources Required, Timeline.
                - WBS: organize by phases (Planning, Design, Development, Testing, Deployment, Maintenance). For each phase list features/modules/tasks as sub-items and estimate hours per task.
                - Cost Estimate: show effort-based estimates per task/phase with hours, hourly rates, and total cost per role (e.g., frontend developer, backend developer, QA, DevOps, UI/UX, PM). Include any third-party tool/license/cloud costs as separate line items.
                - Resources Required: list roles, count of people per role, duration of involvement (weeks or months), and any external services/APIs.
                - Timeline: provide task durations, dependencies, a high-level Gantt-style tabular breakdown, total project duration, and highlight the critical path.
                - Format: use clean headings, tables, and bullet lists. Avoid placeholders unless necessary. Use realistic, justifiable estimates and clearly state assumptions.
                - Deliverable: produce the final estimation document only (no additional commentary).
                
                Scope of Work:
                %s
                
                User feedback (if any):
                %s
                """.formatted(scopeText, feedback == null || feedback.isEmpty() ? "None" : feedback);

        UserMessage message = new UserMessage(promptText);

        MistralAiChatOptions options = MistralAiChatOptions.builder()
                .model(MistralAiApi.ChatModel.LARGE.getValue())
                .temperature(0.4)
                .build();

        Prompt prompt = new Prompt(message, options);
        ChatResponse response = chatModel.call(prompt);


        String resultText;
        try {
            resultText = response.getResult().getOutput().getText();
        } catch (Exception e) {
            resultText = response.getResult().getOutput().toString();
        }

        return resultText.trim();
    }


    private Map<String, String> parseSections(String response) {
        Map<String, String> sections = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(?m)^#{2,}\\s*(.+?)\\s*$(.*?)(?=^#{2,}\\s|\\z)", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String content = matcher.group(2).trim();
            sections.put(title, content);
        }

        if (sections.isEmpty()) {
            sections.put("Estimate", response.trim());
        }
        return sections;
    }


    public ResponseEntity<ByteArrayResource> generateDocxFormatted(String response) {
        Map<String, String> sections = parseSections(response);
        try (XWPFDocument document = new XWPFDocument()) {
            for (Map.Entry<String, String> entry : sections.entrySet()) {
                XWPFParagraph heading = document.createParagraph();
                XWPFRun headingRun = heading.createRun();
                headingRun.setBold(true);
                headingRun.setFontSize(14);
                headingRun.setText(entry.getKey());

                XWPFParagraph para = document.createParagraph();
                XWPFRun run = para.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(12);
                run.setText(entry.getValue());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estimation_report.docx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(out.size())
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }


    public ResponseEntity<ByteArrayResource> generatePdfFormatted(String response) {
        Map<String, String> sections = parseSections(response);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document pdfDoc = new Document();
            PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();
            for (Map.Entry<String, String> entry : sections.entrySet()) {
                Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
                Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
                pdfDoc.add(new Paragraph(entry.getKey(), headingFont));
                pdfDoc.add(new Paragraph(entry.getValue(), contentFont));
                pdfDoc.add(Chunk.NEWLINE);
            }
            pdfDoc.close();
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estimation_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(out.size())
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}

package com.aditya.scopeestimator.controller;

import com.aditya.scopeestimator.dto.EstimationRequest;
import com.aditya.scopeestimator.service.EstimationService;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class EstimationController {

    @Autowired
    private EstimationService estimationService;


    @PostMapping("/estimate/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {

        String scopeText;
        try (PDDocument pdDocument = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            scopeText = stripper.getText(pdDocument);
        }


        String result = estimationService.generateEstimate(scopeText, null);

        return ResponseEntity.ok(result);
    }


    @PostMapping("/estimate/feedback")
    public ResponseEntity<String> regenerateWithFeedback(@RequestBody EstimationRequest request) {
        String feedback = request.getFeedback();
        String previousScope = request.getScopeText();
        String result = estimationService.generateEstimate(
                previousScope != null && !previousScope.isBlank() ? previousScope : "Based on previous scope",
                feedback
        );
        return ResponseEntity.ok(result);
    }


    @PostMapping("/download/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdf(@RequestBody EstimationRequest request) {
        String response = estimationService.generateEstimate(request.getScopeText(), request.getFeedback());
        return estimationService.generatePdfFormatted(response);
    }

    @PostMapping("/download/docx")
    public ResponseEntity<ByteArrayResource> downloadDocx(@RequestBody EstimationRequest request) {
        String response = estimationService.generateEstimate(request.getScopeText(), request.getFeedback());
        return estimationService.generateDocxFormatted(response);
    }
}

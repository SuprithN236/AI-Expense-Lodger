package com.aiexpenseledger.web;

import com.aiexpenseledger.ai.AIService;
import com.aiexpenseledger.ai.ReceiptData;
import com.aiexpenseledger.ai.SpendingQueryRequest;
import com.aiexpenseledger.exception.LedgerValidationException;
import com.aiexpenseledger.security.AuthenticatedUser;
import com.aiexpenseledger.web.dto.ReceiptTextRequest;
import com.aiexpenseledger.web.dto.SpendingQueryResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE, "image/webp");

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SpendingQueryResponse query(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody SpendingQueryRequest request) {
        return new SpendingQueryResponse(aiService.answerSpendingQuery(request, user.id()));
    }

    /** Extracts receipt fields from an uploaded photo. Nothing is written to the ledger. */
    @PostMapping(path = "/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptData extractReceiptFromImage(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new LedgerValidationException("The uploaded receipt file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new LedgerValidationException("Receipt must be a JPEG, PNG, GIF or WebP image");
        }
        return aiService.extractReceiptFromImage(file.getBytes(), MimeType.valueOf(contentType), user.id());
    }

    /** Extracts receipt fields from pasted receipt text. Nothing is written to the ledger. */
    @PostMapping(path = "/receipt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReceiptData extractReceiptFromText(@AuthenticationPrincipal AuthenticatedUser user,
                                              @Valid @RequestBody ReceiptTextRequest request) {
        return aiService.extractReceiptFromText(request.text(), user.id());
    }
}

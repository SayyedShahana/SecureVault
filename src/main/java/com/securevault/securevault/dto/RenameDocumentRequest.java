package com.securevault.securevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameDocumentRequest(
    @NotBlank(message = "{document.rename.filename-required}")
        @Size(max = 255, message = "{document.rename.filename-too-long}")
        String newFileName) {}

package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.mapper.DocumentMapper;
import com.sap.documentssystem.model.Document;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditService;


    @Transactional
    public DocumentResponse createDocument(String title) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Document document = Document.builder()
                .id(UUID.randomUUID())
                .title(title)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        documentRepository.save(document);

        auditService.log(
                user,
                "CREATE_DOCUMENT",
                "DOCUMENT",
                document.getId()
        );

        return DocumentMapper.toResponse(document);
    }
}
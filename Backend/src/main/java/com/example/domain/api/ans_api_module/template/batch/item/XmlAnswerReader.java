package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.company_module.CompanyDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@StepScope
public class XmlAnswerReader implements AnswerFileReader {

    private static final int MAX_XML_DEPTH = 10;
    private static final long MAX_XML_RECORDS = 10_000;

    private final ValidationUtils validationUtils;
    private final CompanyRepository companyRepository;

    @Value("#{jobParameters['companyId']}")
    private Long jobCompanyId;

    @Value("#{jobParameters['category']}")
    private String jobCategory;

    @Override
    public List<PredefinedAnswerUploadDto> read(File file) {
        if (jobCompanyId == null) {
            throw new IllegalStateException("Company ID must be provided in job parameters");
        }

        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);

        Company company = companyRepository.findById(Math.toIntExact(jobCompanyId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Company with id " + jobCompanyId + " not found"));

        CompanyDto companyDto = CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .contactEmail(company.getContactEmail())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        configureXmlSecurity(factory);

        try (InputStream is = new FileInputStream(file)) {
            XMLStreamReader reader = factory.createXMLStreamReader(is);
            long recordCount = 0;
            int currentDepth = 0;
            PredefinedAnswerUploadDto currentDto = null;
            String currentElement = null;

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        currentDepth++;
                        if (currentDepth > MAX_XML_DEPTH) {
                            throw new XMLStreamException("XML depth limit exceeded: " + MAX_XML_DEPTH);
                        }

                        currentElement = reader.getLocalName();
                        if ("answer".equals(currentElement)) {
                            if (++recordCount > MAX_XML_RECORDS) {
                                throw new XMLStreamException("Max records limit exceeded: " + MAX_XML_RECORDS);
                            }
                            currentDto = new PredefinedAnswerUploadDto();
                            currentDto.setCompanyDto(companyDto);
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if (currentDto != null && currentElement != null) {
                            String text = reader.getText().trim();
                            if (!text.isEmpty()) {
                                populateDtoField(currentDto, currentElement, text);
                            }
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        currentDepth--;
                        if ("answer".equals(reader.getLocalName())) {
                            if (currentDto != null) {
                                // Используем категорию из jobParameters только если она не задана в XML
                                if (currentDto.getCategory() == null && jobCategory != null) {
                                    currentDto.setCategory(jobCategory);
                                }

                                validationUtils.validateAnswerDto(currentDto);
                                result.add(currentDto);
                            }
                            currentDto = null;
                        }
                        currentElement = null;
                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new FileProcessingException("XML processing error", e);
        } catch (Exception e) {
            throw new FileProcessingException("Failed to read XML file", e);
        }

        return result;
    }

    private void configureXmlSecurity(XMLInputFactory factory) {
        try {
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to configure XML parser security", e);
        }
    }

    private void populateDtoField(PredefinedAnswerUploadDto dto, String element, String value) {
        switch (element) {
            case "category":
                dto.setCategory(value);
                break;
            case "title":
                dto.setTitle(value);
                break;
            case "answer":
                dto.setAnswer(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.XML;
    }
}

package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class XmlAnswerReader implements AnswerFileReader {

    private static final int MAX_XML_DEPTH = 10;
    private static final long MAX_XML_RECORDS = 10_000;

    private final ValidationUtils validationUtils;

    @Override
    public List<PredefinedAnswerUploadDto> read(MultipartFile file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        configureXmlSecurity(factory);

        try (InputStream is = file.getInputStream()) {
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
                            Objects.requireNonNull(currentDto, "Current DTO cannot be null at this point");
                            validationUtils.validateAnswerDto(currentDto);
                            result.add(currentDto);
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
            case "companyId":
                dto.setCompanyId(Integer.parseInt(value));
                break;
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

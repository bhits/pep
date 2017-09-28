package gov.samhsa.c2s.pep.service;

import feign.FeignException;
import gov.samhsa.c2s.common.document.converter.DocumentXmlConverter;
import gov.samhsa.c2s.common.document.transformer.XmlTransformer;
import gov.samhsa.c2s.common.log.Logger;
import gov.samhsa.c2s.common.log.LoggerFactory;
import gov.samhsa.c2s.pep.infrastructure.ContextHandlerService;
import gov.samhsa.c2s.pep.infrastructure.DssService;
import gov.samhsa.c2s.pep.infrastructure.dto.DSSRequest;
import gov.samhsa.c2s.pep.infrastructure.dto.DSSResponse;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlRequestDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResponseDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResult;
import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseWithDocumentDto;
import gov.samhsa.c2s.pep.service.exception.DssClientInterfaceException;
import gov.samhsa.c2s.pep.service.exception.InvalidDocumentException;
import gov.samhsa.c2s.pep.service.exception.NoDocumentFoundException;
import gov.samhsa.c2s.pep.service.exception.PepException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class PolicyEnforcementPointServiceImpl implements PolicyEnforcementPointService {

    private static final String PERMIT = "permit";
    private final static String CDA_XSL_ENGLISH = "CDA_ENGLISH.xsl";
    private final static String CDA_XSL_SPANISH = "CDA_SPANISH.xsl";
    private static final String ENGLISH_CODE = "en";
    private static final String SPANISH_CODE = "es";

    private final Logger logger = LoggerFactory.getLogger(PolicyEnforcementPointServiceImpl.class);

    private final DocumentXmlConverter documentXmlConverter;

    private final XmlTransformer xmlTransformer;

    private final ContextHandlerService contextHandler;

    private final DssService dssService;

    @Autowired
    public PolicyEnforcementPointServiceImpl(DocumentXmlConverter documentXmlConverter, XmlTransformer xmlTransformer, ContextHandlerService contextHandler, DssService dssService) {
        this.documentXmlConverter = documentXmlConverter;
        this.xmlTransformer = xmlTransformer;
        this.contextHandler = contextHandler;
        this.dssService = dssService;
    }

    @Override
    public AccessResponseDto accessDocument(AccessRequestDto accessRequest, Optional<Boolean> getSegmentedDocumentAsHTML) {
        logger.info("Initiating PolicyEnforcementPointService.accessDocument flow");
        final XacmlRequestDto xacmlRequest = accessRequest.getXacmlRequest();
        logger.debug(xacmlRequest::toString);
        final XacmlResponseDto xacmlResponse = enforcePolicy(xacmlRequest);
        final XacmlResult xacmlResult = XacmlResult.from(xacmlRequest, xacmlResponse);
        logger.debug(xacmlResult::toString);

        assertPDPPermitDecision(xacmlResponse);

        if (accessRequest.getDocument().isPresent()) {
            final DSSRequest dssRequest = accessRequest.toDSSRequest(xacmlResult);
            logger.debug(dssRequest::toString);
            DSSResponse dssResponse;
            try {
                logger.debug("Invoking dss feign client - Start");
                dssResponse = dssService.segmentDocument(dssRequest);
                logger.debug("Invoking dss feign client - End");
            }
            catch (FeignException fe) {
                int causedByStatus = fe.status();

                switch (causedByStatus) {
                    case 400:
                        logger.error("DSS client returned a 400 - BAD REQUEST status, indicating invalid document was passed" +
                                " to DSS client", fe);
                        throw new InvalidDocumentException("Invalid document was passed to DSS client");
                    default:
                        logger.error("DSS client returned an unexpected instance of FeignException", fe);
                        throw new DssClientInterfaceException("An unknown error occurred while attempting to communicate " +
                                "with" +
                                " DSS service");
                }
            }
            logger.debug(dssResponse::toString);
            AccessResponseWithDocumentDto accessResponseWithDocument = (AccessResponseWithDocumentDto) AccessResponseWithDocumentDto.from(dssResponse, xacmlResponse);
            if(getSegmentedDocumentAsHTML.isPresent() && getSegmentedDocumentAsHTML.get()){
                logger.info("Returning XML as well as HTML format of the segmented document");
                accessResponseWithDocument.setSegmentedDocumentAsHTML(Optional.of(convertSegmentedDocumentXmlToHtml (accessResponseWithDocument.getSegmentedDocument(), accessResponseWithDocument.getSegmentedDocumentEncoding())));
            }
            logger.debug(accessResponseWithDocument::toString);
            logger.info("Completed PolicyEnforcementPointService.accessDocument flow, returning response");
            return accessResponseWithDocument;
        } else {
            final AccessResponseDto accessResponse = AccessResponseDto.from(xacmlResponse);
            logger.debug(accessResponse::toString);
            return accessResponse;
        }
    }

    private byte[] convertSegmentedDocumentXmlToHtml (byte[] segmentedDocument, String encoding) {

        final Charset encodingCharset = StringUtils.hasText(encoding) ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        final String segmentedClinicalDocument = new String(segmentedDocument, encodingCharset);
        final Document xmlDoc = documentXmlConverter.loadDocument(segmentedClinicalDocument);

        // xslt transformation
        final String xslUrl = Thread.currentThread().getContextClassLoader().getResource(getLocaleSpecificCdaXSL()).toString();
        final String output = xmlTransformer.transform(xmlDoc, xslUrl, Optional.empty(), Optional.empty());
        return output.getBytes(encodingCharset);
    }

    private static String getLocaleSpecificCdaXSL() {
        Locale selectedLocale = getLocaleFromContext();
        switch (selectedLocale.getLanguage()) {
            case ENGLISH_CODE:
                return CDA_XSL_ENGLISH;
            case SPANISH_CODE:
                return CDA_XSL_SPANISH;
            default:
                return CDA_XSL_ENGLISH;
        }
    }

    private static Locale getLocaleFromContext() {
        if (LocaleContextHolder.getLocale().getLanguage().isEmpty()) {
            return Locale.US;
        } else {
            return LocaleContextHolder.getLocale();
        }
    }

    private void assertPDPPermitDecision(XacmlResponseDto xacmlResponse) {
        Optional.of(xacmlResponse)
                .map(XacmlResponseDto::getPdpDecision)
                .filter(PERMIT::equalsIgnoreCase)
                .orElseThrow(NoDocumentFoundException::new);
    }

    private XacmlResponseDto enforcePolicy(XacmlRequestDto xacmlRequest) {
        logger.debug("Invoking context-handler feign client - Start");
        XacmlResponseDto xacmlResponseDto;
        try {
            xacmlResponseDto = contextHandler.enforcePolicy(xacmlRequest);
        }
        catch (FeignException fe) {
            int causedByStatus = fe.status();

            switch (causedByStatus) {
                case 404:
                    logger.info("consent not found");
                    logger.debug(fe.getMessage(), fe);
                    throw new NoDocumentFoundException();
                default:
                    logger.error(fe.getMessage(), fe);
                    throw new PepException(fe);
            }
        }
        logger.debug(() -> "Invoking context-handler feign client - End" + xacmlResponseDto.toString());

        return xacmlResponseDto;
    }

}

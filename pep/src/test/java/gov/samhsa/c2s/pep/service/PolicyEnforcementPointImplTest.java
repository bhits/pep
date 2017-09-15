package gov.samhsa.c2s.pep.service;

import feign.FeignException;
import gov.samhsa.c2s.pep.infrastructure.ContextHandlerService;
import gov.samhsa.c2s.pep.infrastructure.DssService;
import gov.samhsa.c2s.pep.infrastructure.dto.DSSResponse;
import gov.samhsa.c2s.pep.infrastructure.dto.PatientIdDto;
import gov.samhsa.c2s.pep.infrastructure.dto.SubjectPurposeOfUse;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlRequestDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResponseDto;
import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseWithDocumentDto;
import gov.samhsa.c2s.pep.service.exception.PepException;
import gov.samhsa.c2s.pep.service.exception.NoDocumentFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static gov.samhsa.c2s.common.unit.matcher.ArgumentMatchers.matching;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyEnforcementPointImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ContextHandlerService contextHandler;

    @Mock
    private DssService dssService;

    @InjectMocks
    private PolicyEnforcementPointServiceImpl sut;

    @Test
    public void accessDocument_When_PDP_Decision_Is_Permit() throws Exception {
        // Arrange
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String decision = "permit";
        final List<String> pdpObligations = Arrays.asList("ETH", "GDIS", "HIV", "PSY", "SEX", "ALC", "COM", "ADD");
        final XacmlResponseDto xacmlResponse = XacmlResponseDto.builder().pdpDecision(decision).pdpObligations(pdpObligations).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(documentEncoding);
        final String segmentedDocument = "segmentedDocument";
        final byte[] segmentedDocumentBytes = segmentedDocument.getBytes(documentEncoding);
        final DSSResponse dssResponse = DSSResponse.builder().encoding(documentEncoding.name()).segmentedDocument(segmentedDocumentBytes).build();
        when(contextHandler.enforcePolicy(xacmlRequest)).thenReturn(xacmlResponse);
        when(dssService.segmentDocument(argThat(matching(
                dssRequest -> document.equals(new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))))).thenReturn(dssResponse);
        final AccessRequestDto request = AccessRequestDto.builder().xacmlRequest(xacmlRequest).document(Optional.of(documentBytes)).documentEncoding(Optional.of(documentEncoding.name())).build();

        // Act
        final AccessResponseDto response = sut.accessDocument(request, Optional.empty());

        // Assert
        assertTrue(response instanceof AccessResponseWithDocumentDto);
        assertNotNull(response);
        assertEquals(segmentedDocument, new String(((AccessResponseWithDocumentDto)response).getSegmentedDocument(), documentEncoding));
        verify(contextHandler, times(1)).enforcePolicy(argThat(matching(
                xacmlRequestDto -> recipientNpi.equals(xacmlRequest.getRecipientNpi()) &&
                        intermediaryNpi.equals(xacmlRequest.getIntermediaryNpi()) &&
                        purposeOfUse.equals(xacmlRequest.getPurposeOfUse()) &&
                        patientId.equals(xacmlRequest.getPatientId())
        )));
        verify(dssService, times(1)).segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))));
    }

    @Test
    public void accessDocument_When_PDP_Decision_Is_Deny() throws Exception {
        // Arrange
        thrown.expect(NoDocumentFoundException.class);
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String decision = "deny";
        final List<String> pdpObligations = Arrays.asList("ETH", "GDIS", "HIV", "PSY", "SEX", "ALC", "COM", "ADD");
        final XacmlResponseDto xacmlResponse = XacmlResponseDto.builder().pdpDecision(decision).pdpObligations(pdpObligations).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(documentEncoding);
        final String segmentedDocument = "segmentedDocument";
        final byte[] segmentedDocumentBytes = segmentedDocument.getBytes(documentEncoding);
        final DSSResponse dssResponse = DSSResponse.builder().encoding(documentEncoding.name()).segmentedDocument(segmentedDocumentBytes).build();
        when(contextHandler.enforcePolicy(xacmlRequest)).thenReturn(xacmlResponse);
        when(dssService.segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))))).thenReturn(dssResponse);
        final AccessRequestDto request = AccessRequestDto.builder().xacmlRequest(xacmlRequest).document(Optional.of(documentBytes)).documentEncoding(Optional.of(documentEncoding.name())).build();

        // Act
        final AccessResponseDto response = sut.accessDocument(request, Optional.empty());

        // Assert
        assertTrue(response instanceof AccessResponseWithDocumentDto);
        assertNotNull(response);
        assertEquals(segmentedDocument, new String(((AccessResponseWithDocumentDto)response).getSegmentedDocument(), documentEncoding));
        verify(contextHandler, times(1)).enforcePolicy(argThat(matching(
                xacmlRequestDto -> recipientNpi.equals(xacmlRequest.getRecipientNpi()) &&
                        intermediaryNpi.equals(xacmlRequest.getIntermediaryNpi()) &&
                        purposeOfUse.equals(xacmlRequest.getPurposeOfUse()) &&
                        patientId.equals(xacmlRequest.getPatientId())
        )));
        verify(dssService, times(0)).segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))));
    }

    @Test
    public void accessDocument_When_Context_Hander_Returns_Not_Found_Status() throws Exception {
        // Arrange
        thrown.expect(NoDocumentFoundException.class);
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String decision = "deny";
        final List<String> pdpObligations = Arrays.asList("ETH", "GDIS", "HIV", "PSY", "SEX", "ALC", "COM", "ADD");
        final XacmlResponseDto xacmlResponse = XacmlResponseDto.builder().pdpDecision(decision).pdpObligations(pdpObligations).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(documentEncoding);
        final String segmentedDocument = "segmentedDocument";
        final byte[] segmentedDocumentBytes = segmentedDocument.getBytes(documentEncoding);
        final DSSResponse dssResponse = DSSResponse.builder().encoding(documentEncoding.name()).segmentedDocument(segmentedDocumentBytes).build();
        FeignException e = mock(FeignException.class);
        when(e.status()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(contextHandler.enforcePolicy(xacmlRequest)).thenThrow(e);
        when(dssService.segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))))).thenReturn(dssResponse);
        final AccessRequestDto request = AccessRequestDto.builder().xacmlRequest(xacmlRequest).document(Optional.of(documentBytes)).documentEncoding(Optional.of(documentEncoding.name())).build();

        // Act
        final AccessResponseDto response = sut.accessDocument(request, Optional.empty());

        // Assert
        assertNotNull(response);
        assertTrue(response instanceof  AccessResponseWithDocumentDto);
        assertEquals(segmentedDocument, new String(((AccessResponseWithDocumentDto)response).getSegmentedDocument(), documentEncoding));
        verify(contextHandler, times(1)).enforcePolicy(argThat(matching(
                xacmlRequestDto -> recipientNpi.equals(xacmlRequest.getRecipientNpi()) &&
                        intermediaryNpi.equals(xacmlRequest.getIntermediaryNpi()) &&
                        purposeOfUse.equals(xacmlRequest.getPurposeOfUse()) &&
                        patientId.equals(xacmlRequest.getPatientId())
        )));
        verify(dssService, times(0)).segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))));
    }

    @Test
    public void accessDocument_When_Context_Hander_Returns_Internal_Server_Error_Status() throws Exception {
        // Arrange
        thrown.expect(PepException.class);
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String decision = "deny";
        final List<String> pdpObligations = Arrays.asList("ETH", "GDIS", "HIV", "PSY", "SEX", "ALC", "COM", "ADD");
        final XacmlResponseDto xacmlResponse = XacmlResponseDto.builder().pdpDecision(decision).pdpObligations(pdpObligations).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(documentEncoding);
        final String segmentedDocument = "segmentedDocument";
        final byte[] segmentedDocumentBytes = segmentedDocument.getBytes(documentEncoding);
        final DSSResponse dssResponse = DSSResponse.builder().encoding(documentEncoding.name()).segmentedDocument(segmentedDocumentBytes).build();
        FeignException e = mock(FeignException.class);
        when(e.status()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(contextHandler.enforcePolicy(xacmlRequest)).thenThrow(e);
        when(dssService.segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))))).thenReturn(dssResponse);
        final AccessRequestDto request = AccessRequestDto.builder().xacmlRequest(xacmlRequest).document(Optional.of(documentBytes)).documentEncoding(Optional.of(documentEncoding.name())).build();

        // Act
        final AccessResponseDto response = sut.accessDocument(request, Optional.empty());

        // Assert
        assertNotNull(response);
        assertTrue(response instanceof AccessResponseWithDocumentDto);
        assertEquals(segmentedDocument, new String(((AccessResponseWithDocumentDto)response).getSegmentedDocument(), documentEncoding));
        verify(contextHandler, times(1)).enforcePolicy(argThat(matching(
                xacmlRequestDto -> recipientNpi.equals(xacmlRequest.getRecipientNpi()) &&
                        intermediaryNpi.equals(xacmlRequest.getIntermediaryNpi()) &&
                        purposeOfUse.equals(xacmlRequest.getPurposeOfUse()) &&
                        patientId.equals(xacmlRequest.getPatientId())
        )));
        verify(dssService, times(0)).segmentDocument(argThat(matching(
                dssRequest -> document.equals(
                        new String(dssRequest.getDocument(), documentEncoding)) &&
                        documentEncoding.name().equals(dssRequest.getDocumentEncoding()) &&
                        purposeOfUse.equals(dssRequest.getXacmlResult().getSubjectPurposeOfUse()) &&
                        decision.equals(dssRequest.getXacmlResult().getPdpDecision()) &&
                        extension.equals(dssRequest.getXacmlResult().getPatientId()) &&
                        root.equals(dssRequest.getXacmlResult().getHomeCommunityId()) &&
                        pdpObligations.containsAll(dssRequest.getXacmlResult().getPdpObligations()) &&
                        dssRequest.getXacmlResult().getPdpObligations().containsAll(pdpObligations))));
    }
}
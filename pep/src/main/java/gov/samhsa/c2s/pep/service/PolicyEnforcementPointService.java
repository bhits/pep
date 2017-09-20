package gov.samhsa.c2s.pep.service;

import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;

import java.util.Optional;

public interface PolicyEnforcementPointService {

    AccessResponseDto accessDocument(AccessRequestDto accessRequest, Optional<Boolean> getSegmentedDocumentAsHTML);
}

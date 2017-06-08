package gov.samhsa.c2s.pep.service;

import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import gov.samhsa.c2s.pep.service.dto.SegmentedDocumentsResponseDto;

public interface PolicyEnforcementPointService {

    AccessResponseDto accessDocument(AccessRequestDto accessRequest);
}

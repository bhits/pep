package gov.samhsa.c2s.pep.web;

import gov.samhsa.c2s.pep.service.PolicyEnforcementPointService;
import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
public class PolicyEnforcementPointRestController {

    private final PolicyEnforcementPointService policyEnforcementPointService;

    @Autowired
    public PolicyEnforcementPointRestController(PolicyEnforcementPointService policyEnforcementPointService) {
        this.policyEnforcementPointService = policyEnforcementPointService;
    }

    @RequestMapping(value = "/access", method = RequestMethod.POST)
    public AccessResponseDto access(@Valid @RequestBody AccessRequestDto accessRequest,
                                    @RequestParam Optional<Boolean> getSegmentedDocumentAsHTML) {
        return policyEnforcementPointService.accessDocument(accessRequest, getSegmentedDocumentAsHTML);
    }
}

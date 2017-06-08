package gov.samhsa.c2s.pep.web;

import gov.samhsa.c2s.common.log.Logger;
import gov.samhsa.c2s.common.log.LoggerFactory;
import gov.samhsa.c2s.pep.service.PolicyEnforcementPointService;
import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class PolicyEnforcementPointRestController {

    private final Logger logger = LoggerFactory.getLogger(this);

    @Autowired
    private PolicyEnforcementPointService policyEnforcementPointService;

    @RequestMapping(value = "/access", method = RequestMethod.POST)
    public AccessResponseDto access(@Valid @RequestBody AccessRequestDto accessRequest) {
        return policyEnforcementPointService.accessDocument(accessRequest);
    }

}

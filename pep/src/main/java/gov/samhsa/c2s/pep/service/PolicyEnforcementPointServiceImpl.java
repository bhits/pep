package gov.samhsa.c2s.pep.service;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import gov.samhsa.c2s.pep.infrastructure.ContextHandlerService;
import gov.samhsa.c2s.pep.infrastructure.DssService;
import gov.samhsa.c2s.pep.infrastructure.dto.DSSRequest;
import gov.samhsa.c2s.pep.infrastructure.dto.DSSResponse;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlRequestDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResponseDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResult;
import gov.samhsa.c2s.pep.service.dto.AccessRequestDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import gov.samhsa.c2s.pep.service.exception.DssClientInterfaceException;
import gov.samhsa.c2s.pep.service.exception.InternalServerErrorException;
import gov.samhsa.c2s.pep.service.exception.InvalidDocumentException;
import gov.samhsa.c2s.pep.service.exception.NoDocumentFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PolicyEnforcementPointServiceImpl implements PolicyEnforcementPointService {

    private static final String PERMIT = "permit";


    private final ContextHandlerService contextHandler;
    private final DssService dssService;

    public PolicyEnforcementPointServiceImpl(ContextHandlerService contextHandler, DssService dssService) {
        this.contextHandler = contextHandler;
        this.dssService = dssService;
    }


    @Override
    public AccessResponseDto accessDocument(AccessRequestDto accessRequest) {
        log.info("Initiating PolicyEnforcementPointService.accessDocument flow");
        log.debug(accessRequest.toString());
        final XacmlRequestDto xacmlRequest = accessRequest.getXacmlRequest();
        log.debug(xacmlRequest.toString());
        final XacmlResponseDto xacmlResponse = enforcePolicy(xacmlRequest);
        log.debug(xacmlResponse.toString());
        final XacmlResult xacmlResult = XacmlResult.from(xacmlRequest, xacmlResponse);
        log.debug(xacmlResult.toString());

        assertPDPPermitDecision(xacmlResponse);

        final DSSRequest dssRequest = accessRequest.toDSSRequest(xacmlResult);
        log.debug(dssRequest.toString());
        DSSResponse dssResponse;
        try {
            dssResponse = dssService.segmentDocument(dssRequest);
        } catch (HystrixRuntimeException hystrixErr) {
            Throwable causedBy = hystrixErr.getCause();

            if (!(causedBy instanceof FeignException)) {
                log.error("Unexpected instance of HystrixRuntimeException has occurred", hystrixErr);
                throw new DssClientInterfaceException("An unknown error occurred while attempting to communicate with" +
                        " DSS service");
            }

            int causedByStatus = ((FeignException) causedBy).status();

            switch (causedByStatus) {
                case 400:
                    log.error("DSS client returned a 400 - BAD REQUEST status, indicating invalid document was passed" +
                            " to DSS client", causedBy);
                    throw new InvalidDocumentException("Invalid document was passed to DSS client");
                default:
                    log.error("DSS client returned an unexpected instance of FeignException", causedBy);
                    throw new DssClientInterfaceException("An unknown error occurred while attempting to communicate " +
                            "with" +
                            " DSS service");
            }
        }
        log.debug(dssResponse.toString());
        final AccessResponseDto accessResponse = AccessResponseDto.from(dssResponse);
        log.debug(accessResponse.toString());
        log.info("Completed PolicyEnforcementPointService.accessDocument flow, returning response");
        return accessResponse;
    }


    private void assertPDPPermitDecision(XacmlResponseDto xacmlResponse) {
        Optional.of(xacmlResponse)
                .map(XacmlResponseDto::getPdpDecision)
                .filter(PERMIT::equalsIgnoreCase)
                .orElseThrow(NoDocumentFoundException::new);
    }

    private XacmlResponseDto enforcePolicy(XacmlRequestDto xacmlRequest) {
        try {
            return contextHandler.enforcePolicy(xacmlRequest);
        } catch (HystrixRuntimeException e) {
            final FeignException feignException = Optional.of(e)
                    .map(HystrixRuntimeException::getCause)
                    .filter(FeignException.class::isInstance)
                    .map(FeignException.class::cast)
                    .orElseThrow(() -> {
                        log.error(e.getMessage(), e);
                        return new InternalServerErrorException(e);
                    });
            if (HttpStatus.NOT_FOUND.equals(getHttpStatus(feignException))) {
                log.info("consent not found");
                log.debug(e.getMessage(), e);
                throw new NoDocumentFoundException();
            } else {
                log.error(e.getMessage(), e);
                throw new InternalServerErrorException(e);
            }
        }
    }

    private HttpStatus getHttpStatus(FeignException e) {
        return HttpStatus.valueOf(e.status());
    }


}

package gov.samhsa.c2s.pep.service.dto;

import gov.samhsa.c2s.pep.infrastructure.dto.DSSRequest;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlRequestDto;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResult;
import gov.samhsa.c2s.pep.service.exception.DocumentNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "document")
public class AccessRequestDto {
    @NotNull
    private XacmlRequestDto xacmlRequest;

    @NotNull
    @UnwrapValidatedValue(false)
    private Optional<byte[]> document;

    @NotNull
    @UnwrapValidatedValue(false)
    private Optional<String> documentEncoding;

    public DSSRequest toDSSRequest(XacmlResult xacmlResult) {
        return DSSRequest.builder()
                .document(getDocument().orElseThrow(DocumentNotFoundException::new))
                .documentEncoding(getDocumentEncoding().orElse(StandardCharsets.UTF_8.name()))
                .xacmlResult(xacmlResult.toBuilder().build())
                .build();
    }
}

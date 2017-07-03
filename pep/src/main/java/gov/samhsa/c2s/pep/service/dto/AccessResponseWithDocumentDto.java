package gov.samhsa.c2s.pep.service.dto;

import gov.samhsa.c2s.pep.infrastructure.dto.DSSResponse;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true, exclude = "segmentedDocument")
@EqualsAndHashCode(callSuper = true)
public class AccessResponseWithDocumentDto extends AccessResponseDto {

    @NotEmpty
    private byte[] segmentedDocument;

    @NotBlank
    private String segmentedDocumentEncoding;

    @Builder
    public AccessResponseWithDocumentDto(String decision, byte[] segmentedDocument, String segmentedDocumentEncoding) {
        this.decision = decision;
        this.segmentedDocument = segmentedDocument;
        this.segmentedDocumentEncoding = segmentedDocumentEncoding;
    }

    public static AccessResponseDto from(DSSResponse dssResponse) {
        return AccessResponseWithDocumentDto.builder()
                .segmentedDocument(dssResponse.getSegmentedDocument())
                .segmentedDocumentEncoding(dssResponse.getEncoding())
                .build();
    }

    public static AccessResponseDto from(DSSResponse dssResponse, XacmlResponseDto xacmlResponse) {
        return AccessResponseWithDocumentDto.builder()
                .segmentedDocument(dssResponse.getSegmentedDocument())
                .segmentedDocumentEncoding(dssResponse.getEncoding())
                .decision(xacmlResponse.getPdpDecision())
                .build();
    }
}

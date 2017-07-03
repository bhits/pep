package gov.samhsa.c2s.pep.service.dto;

import gov.samhsa.c2s.pep.infrastructure.dto.XacmlResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.util.Assert;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class AccessResponseDto {

    @NotBlank
    protected String decision;

    public static AccessResponseDto from(XacmlResponseDto xacmlResponse) {
        Assert.hasText(xacmlResponse.getPdpDecision(), "PDP Decision must have text");
        return AccessResponseDto.of(xacmlResponse.getPdpDecision());
    }
}

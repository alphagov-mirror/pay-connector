package uk.gov.pay.connector.gateway.epdq.model.response;

import uk.gov.pay.connector.gateway.model.response.BaseResponse;

import javax.xml.bind.annotation.XmlAttribute;

import static org.apache.commons.lang3.StringUtils.trim;

public abstract class EpdqBaseResponse implements BaseResponse {

    @XmlAttribute(name = "NCERROR")
    private String errorCode;

    @XmlAttribute(name = "NCERRORPLUS")
    private String errorMessage;

    @Override
    public String getErrorCode() {
        return trim(errorCode);
    }

    @Override
    public String getErrorMessage() {
        return trim(errorMessage);
    }

    @Override
    public abstract String toString();

}

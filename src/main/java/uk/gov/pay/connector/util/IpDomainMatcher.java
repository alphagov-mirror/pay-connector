package uk.gov.pay.connector.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

import static java.lang.String.format;

public class IpDomainMatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(IpDomainMatcher.class);
    
    private ReverseDnsLookup reverseDnsLookup;

    @Inject
    public IpDomainMatcher(ReverseDnsLookup reverseDnsLookup) {
        this.reverseDnsLookup = reverseDnsLookup;
    }

    private String extractForwardedIp(String forwardedAddress) {
        String extractedIp = forwardedAddress.split(",")[0];
        logger.debug("Extracted ip {} from X-Forwarded-For '{}'", extractedIp, forwardedAddress);
        return extractedIp;
    }
    
    public boolean ipMatchesDomain(String forwardedAddress, String domain) {
        try {
            String ipAddress = extractForwardedIp(forwardedAddress);
            Optional<String> host = reverseDnsLookup.lookup(ipAddress);
            if (host.isEmpty()) {
                throw new Exception(format("Host not found for ip address '%s'", ipAddress));
            }
            if (!host.get().endsWith(domain + ".")) {
                logger.error("Reverse DNS lookup on ip '{}' - resolved domain '{}' does not match '{}'", ipAddress, host.get(), domain);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Reverse DNS Lookup failed: {}", e.getLocalizedMessage());
            return false;
        }
    }
}

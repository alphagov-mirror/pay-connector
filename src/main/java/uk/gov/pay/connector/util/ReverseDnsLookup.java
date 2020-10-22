package uk.gov.pay.connector.util;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import static java.lang.String.join;

public class ReverseDnsLookup {

    public Optional<String> lookup(String hostIp) {
        List<String> components = Arrays.asList(hostIp.split("\\."));
        Collections.reverse(components);
        String reverseIp = join(".", components.toArray(new String[0])) + ".in-addr.arpa";
        try {
            Hashtable env = new Hashtable();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(reverseIp, new String[]{"PTR"});
            ctx.close();
            return Optional.ofNullable(attrs.get("ptr").get().toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

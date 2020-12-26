package ca.ubc.cs317.dnslookup.tests;

import ca.ubc.cs317.dnslookup.DNSLookupService;
import ca.ubc.cs317.dnslookup.DNSNode;
import ca.ubc.cs317.dnslookup.DNSQueryHandler;
import ca.ubc.cs317.dnslookup.RecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

public class DNSLookupServiceTests {

    private DNSNode node;
    private InetAddress UBCServer;
    private InetAddress googleServer;
    
    @BeforeEach
    public void setup() {
        try {
            DNSQueryHandler.openSocket();
            UBCServer = InetAddress.getByName("199.7.83.42");
            googleServer = InetAddress.getByName("ns2.google.com");
        } catch (Exception ignore) {}
        String hostName = "www.cs.ubc.ca";
        RecordType type = RecordType.A;
        node = new DNSNode(hostName, type);
        DNSQueryHandler.setVerboseTracing(true);
    }
    
    @Test
    public void testRetrieveResultsFromServer() {
//        DNSLookupService.retrieveResultsFromServer(node, UBCServer);
//        DNSLookupService.retrieveResultsFromServer(new DNSNode("google.com", RecordType.A), googleServer);
        DNSLookupService.retrieveResultsFromServer(new DNSNode("prep.ai.mit.edu", RecordType.A), UBCServer);
    }
}

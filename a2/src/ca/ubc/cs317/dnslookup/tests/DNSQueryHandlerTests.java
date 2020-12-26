package ca.ubc.cs317.dnslookup.tests;

import ca.ubc.cs317.dnslookup.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;


public class DNSQueryHandlerTests {
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
    public void testBuildAndSendQuery() {
        System.out.print("===================Start of testBuildAndSendQuery()================\n");
        byte[] msg = new byte[256];
        try {
            DNSServerResponse dnsServerResponse = DNSQueryHandler.buildAndSendQuery(msg, UBCServer, node);
            ByteBuffer buffer = dnsServerResponse.getResponse();
            assertNotNull(dnsServerResponse);
            assertNotNull(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecodeAndCacheResponse() {
        System.out.print("===================Start of testDecodeAndCacheResponse()================\n");
        buildAndDecode(node, UBCServer);
        try {
            buildAndDecode(node, InetAddress.getByName("199.4.144.2"));
            buildAndDecode(node, InetAddress.getByName("142.103.1.1"));
            buildAndDecode(node, InetAddress.getByName("137.82.61.120"));
            buildAndDecode(node, InetAddress.getByName("198.162.35.1"));
        } catch (Exception ignored) {}
//        buildAndDecode(new DNSNode("google.com", RecordType.A), UBCServer);
//        buildAndDecode(new DNSNode("google.com", RecordType.A), googleServer);
    }

    @Test
    public void testDecodeMX() {
        System.out.print("===================Start of testDecodeMX()================\n");
        buildAndDecode(new DNSNode("google.com", RecordType.MX), UBCServer);
        buildAndDecode(new DNSNode("google.com", RecordType.MX), googleServer);
    }

    @Test
    public void testDecodeSOA() {
        System.out.print("===================Start of testDecodeSOA()================\n");
        buildAndDecode(new DNSNode("google.com", RecordType.SOA), UBCServer);
        buildAndDecode(new DNSNode("google.com", RecordType.SOA), googleServer);
    }
    
    @Test
    public void testDecodeAAAA() {
        System.out.print("===================Start of testDecodeAAAA()================\n");
        try {
            buildAndDecode(new DNSNode("www.cs.ubc.ca", RecordType.AAAA), InetAddress.getByName("198.162.35.1"));
        } catch (Exception ignored) {}
    }

    @Test
    public void testDecodeOther() {
        System.out.print("===================Start of testDecodeAAAA()================\n");
        try {
            buildAndDecode(new DNSNode("www.cs.ubc.ca", RecordType.OTHER), InetAddress.getByName("198.162.35.1"));
        } catch (Exception ignored) {}
    }

    @Test
    public void testDecodeRandom() {
        System.out.print("===================Start of testDecodeAAAA()================\n");
        try {
            buildAndDecode(new DNSNode("asdf", RecordType.OTHER), InetAddress.getByName("198.162.35.1"));
        } catch (Exception ignored) {}
    }
    
    private void buildAndDecode(DNSNode node, InetAddress server) {
        byte[] msg = new byte[256];
        try {
            DNSServerResponse dnsServerResponse = DNSQueryHandler.buildAndSendQuery(msg, server, node);
            DNSCache cache = DNSCache.getInstance();
            ByteBuffer buffer = dnsServerResponse.getResponse();
            int transID = dnsServerResponse.getTransactionID();
            DNSQueryHandler.decodeAndCacheResponse(transID, buffer, cache);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}

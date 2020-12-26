package ca.ubc.cs317.dnslookup;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DNSQueryHandler {

    private static final int DEFAULT_DNS_PORT = 53;
    private static DatagramSocket socket;
    private static boolean verboseTracing = false;

    private static final Random random = new Random();
    
    /**
     * Sets up the socket and set the timeout to 5 seconds
     *
     * @throws SocketException if the socket could not be opened, or if there was an
     *                         error with the underlying protocol
     */
    public static void openSocket() throws SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(5000);
    }

    /**
     * Closes the socket
     */
    public static void closeSocket() {
        socket.close();
    }

    /**
     * Set verboseTracing to tracing
     */
    public static void setVerboseTracing(boolean tracing) {
        verboseTracing = tracing;
    }

    /**
     * Builds the query, sends it to the server, and returns the response.
     *
     * @param message Byte array used to store the query to DNS servers.
     * @param server  The IP address of the server to which the query is being sent.
     * @param node    Host and record type to be used for search.
     * @return A DNSServerResponse Object containing the response buffer and the transaction ID.
     * @throws IOException if an IO Exception occurs
     */
    public static DNSServerResponse buildAndSendQuery(byte[] message, InetAddress server,
                                                      DNSNode node) throws IOException {
        // Initialize the data output stream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream doutputStreams = new DataOutputStream(byteArrayOutputStream);
        
        // Start to encode the header
        
        // Generate the transaction ID
        short transactionID = (short)random.nextInt(Short.MAX_VALUE);
        
        // Write the transaction id into data output stream
        doutputStreams.writeShort(transactionID);

        // Write Query Flags by default is 0x0100: 0 (QR, response), 0000 (OPCODE, standard query), 0 (valid for 
        // response), 0 (TC, the message length is restrict <= 256 bytes), 1 (RD, set recursive query), 0 (RA), 0 (Z), 
        // 0000 (RCODE)
        doutputStreams.writeShort(0x0100);

        // Write the QDCOUNT. There's 1 query question as expected.
        doutputStreams.writeShort(0x0001);

        // Write the ANCOUNT
        doutputStreams.writeShort(0x0000);

        // Write the NSCOUNT
        doutputStreams.writeShort(0x0000);

        // Write the ARCOUNT
        doutputStreams.writeShort(0x0000);
        
        // Start to encode the query question record
        
        // Write the host name
        String[] hostName = node.getHostName().split("\\.");
        for (String s : hostName) {
            byte[] domainBytes = s.getBytes(StandardCharsets.UTF_8);
            doutputStreams.writeByte(domainBytes.length);
            doutputStreams.write(domainBytes);
        }

        // Mark the end of question
        doutputStreams.writeByte(0x00);

        // Write the Type
        doutputStreams.writeShort(node.getType().getCode());
//        doutputStreams.writeShort(11);

        // Write the Class, IN by default
        doutputStreams.writeShort(0x0001);

        // Send the query to DNS server
        message = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(message, message.length, server, DEFAULT_DNS_PORT);
        int tries = 0;
        while(true) {
            try {
                // Verbose print of the summary of query
                if (verboseTracing)
                    System.out.printf("\n\nQuery ID     %d %s %s --> %s%n",
                            transactionID,
                            node.getHostName(),
                            node.getType(),
                            server.getHostAddress());
                socket.send(packet);
                // Await response from DNS server
                byte[] buf = new byte[512];
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(5000);
                socket.receive(receivedPacket);
                ByteBuffer byteBuffer = ByteBuffer.allocate(receivedPacket.getData().length);
                byteBuffer.put(receivedPacket.getData());
                return new DNSServerResponse(byteBuffer, transactionID);
            } catch (SocketTimeoutException e) {
                tries++;
                if (tries >= 2) {
                    return new DNSServerResponse(ByteBuffer.allocate(512), transactionID);
                }
            } 
        }
    }

    /**
     * Decodes the DNS server response and caches it.
     *
     * @param transactionID  Transaction ID of the current communication with the DNS server
     * @param responseBuffer DNS server's response
     * @param cache          To store the decoded server's response
     * @return A set of resource records corresponding to the name servers of the response.
     */
    public static Set<ResourceRecord> decodeAndCacheResponse(int transactionID, ByteBuffer responseBuffer,
                                                             DNSCache cache) {
        // TODO (PART 1): Implement this
        Set<ResourceRecord> result = new HashSet<>();
        responseBuffer.position(0);
        Map<Integer, String> refMap = new HashMap<>();
        try {
            // Decode transaction id of the response, halt if it is mismatched with the input transaction id 
            int responseID = responseBuffer.getShort();
            if (transactionID != responseID) return result;

            // Decode first octet of the flags
            int flags = responseBuffer.getShort();
            boolean authoritative = (flags & 0x0400) != 0;

            // Skip QDCOUNT
            responseBuffer.position(responseBuffer.position()+2);
            
            // Decode the ANCOUNT, NSCOUNT, ARCOUNT
            int[] counts = new int[]{responseBuffer.getShort(), responseBuffer.getShort(), responseBuffer.getShort()};
            String[] answerType = new String[]{"Answers", "Nameservers", "Additional Information"};
            
            // Decode the host name
            String hostName = decodeName(refMap, responseBuffer);
            
            // Skip the QTYPE and QCLASS
            responseBuffer.position(responseBuffer.position()+4);
            
            // Verbose print of response summary
            if (verboseTracing) System.out.printf("Response ID: %d Authoritative = %b%n", responseID, authoritative);
            
            for (int i = 0; i < 3;i++) {
                // Verbose print of records group summary
                if (verboseTracing) System.out.printf("  %s (%d)\n", answerType[i], counts[i]);
                int curCount = counts[i];
                
                // Decode each record
                while(curCount-- > 0) {
                    // NAME
                    String curName = decodeName(refMap, responseBuffer);
                    
                    // TYPE
                    RecordType type = RecordType.getByCode(responseBuffer.getShort());
                    
                    // CLASS
                    int recordClass = responseBuffer.getShort();
                    
                    // TTL
                    long ttl = responseBuffer.getInt() & 0x00000000ffffffffL;
                    
                    // RDLENGTH
                    int rawDataLength = responseBuffer.getShort();
                    
                    // RDATA, different type of record with different strategy.
                    ResourceRecord curResourceRecord = null;
                    switch (type) {
                        case A:
                            curResourceRecord =
                                    new ResourceRecord(curName, type, ttl, Inet4Address.getByName(decodeI4Address(responseBuffer)));
                            break;
                        case AAAA:
                            curResourceRecord =
                                    new ResourceRecord(curName, type, ttl, Inet6Address.getByName(decodeI6Address(responseBuffer)));
                            break;
                        
                        case CNAME:
                        case NS:
                            curResourceRecord = new ResourceRecord(curName, type, ttl, decodeName(refMap, responseBuffer));
                            break;
                        case SOA:
                            int pos = responseBuffer.position();
                            curResourceRecord = new ResourceRecord(curName, type, ttl, decodeName(refMap, responseBuffer));
                            responseBuffer.position(pos+rawDataLength);
                            break;
                        case MX:
                            curResourceRecord = new ResourceRecord(curName, type, ttl, decodeMXRecord(refMap, responseBuffer));
                            break;
                        case OTHER:
                            curResourceRecord = new ResourceRecord(curName, type, ttl, decodeDefault(responseBuffer, rawDataLength));
                            break;
                    }
                    if (i==1) result.add(curResourceRecord);
                    cache.addResult(curResourceRecord);
                    verbosePrintResourceRecord(curResourceRecord, type.getCode());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    

    /**
     * Default printing message when the record type is not supported
     * @param byteBuffer
     * @param length length of the raw data
     * @return 
     */
    public static String decodeDefault(ByteBuffer byteBuffer, int length) {
        length += byteBuffer.position();
        byteBuffer.position(length > 511? 511:length);
        return "Not supported!";
    }
    
    
    /**
     * Decode the MX record raw data
     * @param refMap
     * @param byteBuffer
     * @return
     */
    public static String decodeMXRecord(Map<Integer, String> refMap, ByteBuffer byteBuffer) {
        byteBuffer.getShort();
        return decodeName(refMap, byteBuffer);
    }


    /**
     * Decode the A type record raw data
     * @param byteBuffer
     * @return
     */
    public static String decodeI4Address(ByteBuffer byteBuffer) {
        List<String> labels = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            labels.add(String.valueOf(byteBuffer.get()&0xFF));
        }
        return String.join(".", labels);
    }


    /**
     * Decode the AAAA type record raw data
     * @param byteBuffer
     * @return
     */
    public static String decodeI6Address(ByteBuffer byteBuffer) {
        List<String> labels = new ArrayList<>();
        for(int i = 0; i < 8; i++) {
            labels.add(Integer.toHexString(byteBuffer.getShort() & 0x0000FFFF));
        }
        return String.join(":", labels);
    }

    /**
     * Decode the name part of the record raw data by recursively fetching labels until hit 0x00 or a name ptr
     * @param refMap
     * @param byteBuffer
     * @return
     */
    public static String decodeName(Map<Integer, String> refMap, ByteBuffer byteBuffer) {
        List<String> labels = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        int partLength;
        while(true) {
            positions.add(byteBuffer.position());
            partLength = byteBuffer.get();
            if ((partLength & 0x00C0) == 0x00C0) {
                int start = byteBuffer.position()-1;
                byteBuffer.position(start);
                int nextPos = byteBuffer.getShort() & 0x3fff;
                String label = refMap.get(nextPos);
                labels.add(label);
                break;
            }
            if (partLength == 0) {
                break;
            }
            byte[] text = new byte[partLength];
            byteBuffer.get(text, 0, partLength);
            labels.add(new String(text, StandardCharsets.UTF_8));
        }
        for (int i = 0; i < labels.size(); i++) {
            refMap.put(positions.get(i), String.join(".", labels.subList(i, labels.size())));
        }
        return String.join(".", labels);
    }
    
    /**
     * Formats and prints record details (for when trace is on)
     *
     * @param record The record to be printed
     * @param rtype  The type of the record to be printed
     */
    private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),
                    record.getTextResult());
    }
}


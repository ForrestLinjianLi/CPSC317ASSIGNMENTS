import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import Exception.*;


import static java.lang.Thread.sleep;

public class Command {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String dic = "*";
    private StringBuilder stringBuilder;
    
    private static final String[] DEFAULT_CODE = new String[]{"250", "552"};
    
    public Command(String host, int port) throws ExecuteException, ServerException {
        stringBuilder = new StringBuilder();
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (!readBuffer(false, new String[]{"220"})[0].equals("220")) close();
            stringBuilder.setLength(0);
        } catch (ServerException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecuteException(String.format(CommandUtil.EXCEPTION_LOOKUP.get(920), host, port));
        }
    }

    public void set(String[] args) throws IncorrectNumArgumentException {
        if (args.length != 1) throw new IncorrectNumArgumentException();
        this.dic = args[0];
    }

    public void execDefine(String[] args) throws IncorrectNumArgumentException, IOException, ServerException {
        execCommand(String.format("DEFINE %s ", dic), 1, args);
        String[] status = readBuffer(true, new String[]{"250", "552", "550"});
        if (!status[0].equals("250")) {
            System.out.println("***No definition found***");
            execCommand("MATCH * . ", 1, args);
            status = readBuffer(true, DEFAULT_CODE);
            if (status[0].equals("552")) System.out.println("****No matches found****");
        }
    }

    public void execMatch(String[] args) throws IncorrectNumArgumentException, IOException, ServerException {
        execCommand(String.format("MATCH %s EXACT ", dic), 1, args);
        String[] status = readBuffer(true, DEFAULT_CODE);
        if (status[0].equals("552")) System.out.println("*****No matching word(s) found*****");
    }

    public void execPrefixMatch(String[] args) throws IncorrectNumArgumentException, IOException, ServerException {
        execCommand(String.format("MATCH %s PREFIX ", dic), 1, args);
        String[] status = readBuffer(true, DEFAULT_CODE);
        if (status[0].equals("552")) System.out.println("****No matching word(s) found****");
    }

    public void execDict(String[] args) throws IncorrectNumArgumentException, IOException, ServerException {
        execCommand("SHOW DB", 0, args);
        readBuffer(true, new String[]{"250"});
    }

    public void execQuit(String[] args) throws IncorrectNumArgumentException, IOException, ServerException {
        execCommand("QUIT", 0, args);
        readBuffer(true, new String[]{"221"});
        close();
    }

    public void execCommand(String command, int argNum, String[] args) throws IncorrectNumArgumentException {
        if (args.length != argNum) throw new IncorrectNumArgumentException();
        String cmd = argNum == 0 ? command:command + args[0];
        out.println(cmd);
        if (CSdict.debugOn) stringBuilder.append(String.format("> %s\n", cmd));
    }

    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String[] readBuffer(boolean appendPrefix, String[] validCodes) throws IOException, ServerException {
        boolean debug = CSdict.debugOn;
        String line = "";
        int tries = 5;
        try {
            while (tries > 0) {
                if (in.ready()) {
                    line = in.readLine();
                    if (line.length() == 0 || line.startsWith("#")) 
                        continue;
                    if (line.matches("^\\d{3}\\s.+")) {
                        if (debug) {
                            stringBuilder.append(String.format("<-- %s\n", line));
                        }
                        if (appendPrefix && line.matches("^151\\s.+")) {
                            String[] splits = line.split(" ", 4);
                            stringBuilder.append(String.format("@ %s %s\n", splits[2], splits[3]));
                        }
                        if (line.matches("^(2|4|5).*")) break;
                        continue;
                    } 
                    stringBuilder.append(String.format("%s\n", line.trim()));
                } else {
                    try {
                        sleep(1000);
                        tries--;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (tries == 0) throw new ServerException(String.format(CommandUtil.EXCEPTION_LOOKUP.get(999), 
                    "Timed out while waiting for a response."));
        } catch (IOException e) {
            this.close();
            throw new IOException(e.getMessage());
        } finally {
            String res = stringBuilder.toString().trim();
            if (res.length() != 0) System.out.println(res);
            stringBuilder.setLength(0);
        }
        String[] status = line.split(" ", 2);
        if (status.length == 2) validateStatus(status[0], status[1], validCodes);
        return status;
    }

    private void validateStatus(String statusCode, String msg, String[] codes) throws ServerException {
        for(String code: codes)
            if (code.equals(statusCode))
                return;
        throw new ServerException(String.format(CommandUtil.EXCEPTION_LOOKUP.get(999), msg));
    }
}

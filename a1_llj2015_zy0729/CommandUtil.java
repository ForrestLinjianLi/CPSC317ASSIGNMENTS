import Exception.*;

import java.io.IOException;
import java.util.*;

public class CommandUtil {
    
    private static Command command = null;
    
    public static final Map<Integer, String> EXCEPTION_LOOKUP = new HashMap<Integer, String>(){{
        put(900, "900 Invalid command.");
        put(901, "901 Incorrect number of arguments.");
        put(902, "902 Invalid argument.");
        put(903, "903 Supplied command not expected at this time.");
        put(920, "920 Control connection to %s on port %s failed to open. ");
        put(925, "925 Control connection I/O error, closing control connection.");
        put(996, "996 Too many command line options - Only -d is allowed.");
        put(997, "997 Invalid command line option - Only -d is allowed.");
        put(999, "999 Processing error. %s");
    }};
    
    public static final Set<String> COMMDANDS = new HashSet<>(Arrays.asList("quit","close", "match", "prefixmatch",
            "define", "set", "open", "dict"));

    public static void execute(String commandString, String[] args) throws ExecuteException {
        commandString = commandString.toLowerCase();
        try {
            if (commandString.trim().length() == 0 || commandString.matches("^#.*")) return;
            if (commandString.equals("open")) {
                if (command != null) throw new ExecuteException(EXCEPTION_LOOKUP.get(903));
                open(args);
            } else if (commandString.equals("quit")) {
                if (command != null)
                    command.execQuit(args);
                System.exit(0);
            } else if (command != null)
                switch (commandString) {
                    case "dict":
                        command.execDict(args);
                        break;
                    case "set":
                        command.set(args);
                        break;
                    case "define":
                        command.execDefine(args);
                        break;
                    case "match":
                        command.execMatch(args);
                        break;
                    case "prefixmatch":
                        command.execPrefixMatch(args);
                        break;
                    case "close":
                        command.execQuit(args);
                        command = null;
                        break;
                    default:
                        throw new ExecuteException(EXCEPTION_LOOKUP.get(900));
                }
            else if (COMMDANDS.contains(commandString)) throw new ExecuteException(EXCEPTION_LOOKUP.get(903));
            else throw new ExecuteException(EXCEPTION_LOOKUP.get(900));
        } catch (IncorrectNumArgumentException e) {
            throw new ExecuteException(EXCEPTION_LOOKUP.get(901));
        } catch (IOException e) {
            throw new ExecuteException(EXCEPTION_LOOKUP.get(925));
        } catch (ServerException e) {
            throw new ExecuteException(e.getMessage());
        } catch (ExecuteException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecuteException(String.format(EXCEPTION_LOOKUP.get(999),e.getLocalizedMessage()));
        }
    }
    
    public static void open(String[] args) throws ExecuteException, ServerException {
        if (args.length != 2) throw new ExecuteException(EXCEPTION_LOOKUP.get(901));
        if (!args[0].matches("(^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$)|^((?!-)[A-Za-z0-9-]" +
                "{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$")) 
            throw new ExecuteException(EXCEPTION_LOOKUP.get(902));
        if (!args[1].matches("\\d+")) throw new ExecuteException(EXCEPTION_LOOKUP.get(902));
        
        command = new Command(args[0], Integer.parseInt(args[1]));
    }
}

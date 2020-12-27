# CPSC317ASSIGNMENTS

# A1 DICTIONARY Client
This is a simple shell-style interface to the user. This interface will read lines of inputwith application commands and interpret them according to the description below.

## Commands

### open SERVER PORT
Opens a new TCP/IP connection to an dictionary server.(open dict.org 2628)
### dict
Retrieve and print the list of all the dictionaries the server supports. 
### set DICTIONARY
Set the dictionary to retrieve subsequent definitions and/or matches from.
### define WORD
Retrieve and print all the definitions for WORD.
### match WORD
Retrieve and print all the exact matches for WORD.
### prefixmatch WORD
Retrieve and print all the prefix matches.
### close
### quit

# A2 Domain Name System (DNS) Resolver Client
This is a scaled back DNS resolver client that is implemented in Java and UDP datagrams. It interacts with various DNS servers to resolve domainnames (e.g. www.google.com) into IP addresses. Depending upon the commands provided this client is to resolve names to IPV4 or IPV6 addresses.

## Commands

### quit
Quits the program. The command exit also executes the same thing.

### server servername
Changes the DNS server to start future searches.

### trace on|off
Turns verbose tracing on or off. If tracing is on, the program must print a trace of all thequeries made and responses received before printing any result.

### lookup hostname [type]
Looks up a specific host name (with an optional record type, default A)and prints the resulting IP address. The result may be obtained from a local cache, in which case notracing is printed.

### dump
Prints all currently cached host names and records.

# A3 Simple FTP server
It uses the Unix Socket API to construct a minimal ftp server, called CSftp, capable of interacting with a variety of ftp clients.

## Commands(All based on RFC 959 (https://www.ietf.org/rfc/rfc959.txt))
### USER - (4.1.1)
### QUIT - (4.1.1)
### CWD - (4.1.1) For security reasons you are not accept any CWD command that starts with ./ or ../ or contains ../ in it. (hint see the chdir system call.)
### CDUP - (4.1.1) For security reasons do not allow a CDUP command to set the working directory to be the parent directory of where your ftp server isstarted from. (hint use the getcwd system call to get the initial working directory so that you know where things are started from and then if a CDUPcommand is received while in that diredtory return an appropriate error code to the client.)
### TYPE - (4.1.1) you are only to support the Image and ASCII type (3.1.1, 3.1.1.3)
### MODE - you are only to support Stream mode (3.4.1)
### STRU - you are only to support File structure type (3.1.2, 3.1.2.1)
### RETR - (4.1.3)
### PASV - (4.1.1)
### NLST - (4.1.3) to produce a directory listing, you are

//
//  main.c
//  SimpleServer
//
//  Sample code to illustarate how to use socket system
//  calls to build a simple TCP server in C.
//  For your assignment 3, you don't need to stick to this sample code.
//
//  Created by Jerry Wang on 3/12/20.
//  Copyright © 2020. All rights reserved.
//
//  Compile: gcc -o SimpleServer -pthread SimpleServer.c
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <pthread.h>
#include <regex.h>
#include <ifaddrs.h>
#include "dir.h"
#include "usage.h"

#define SIZE 1024
#define TIMEOUT 30

// Check if the input string start with the input character or not.
bool starts_with(const char *pre, const char *str)
{
  size_t lenpre = strlen(pre),
         lenstr = strlen(str);
  return lenstr < lenpre ? false : memcmp(pre, str, lenpre) == 0;
}

// Send the message to client.
void send_str(int clienfd, char *msg)
{
  if (send(clienfd, msg, strlen(msg), 0) < 0)
  {
    // Failed to send.
    perror("first 220");
  }
}

// Send the correspinding message to client depending on the code.
void send_msg(int clientfd, int code, char *str)
{
  char buf[SIZE];
  switch (code)
  {
  case 150:
    sprintf(buf, "150 File status okay; about to open data connection. %s\r\n", str);
    break;
  case 200:
    sprintf(buf, "200 Command okay. %s\r\n", str);
    break;
  case 220:
    sprintf(buf, "220 Service ready for new user. %s\r\n", str);
    break;
  case 221:
    sprintf(buf, "221 Service closing control connection. %s\r\n", str);
    break;
  case 226:
    sprintf(buf, "226 Closing data connection. %s\r\n", str);
    break;
  case 230:
    sprintf(buf, "230 User logged in, proceed. %s\r\n", str);
    break;
  case 250:
    sprintf(buf, "250 Requested file action okay, completed. %s\r\n", str);
    break;
  case 425:
    sprintf(buf, "425 Can't open data connection. %s\r\n", str);
    break;
  case 426:
    sprintf(buf, "426 Connection closed; transfer aborted. %s\r\n", str);
    break;
  case 450:
    sprintf(buf, "450 Requested file action not taken.File unavailable (e.g., file busy). %s\r\n", str);
    break;
  case 451:
    sprintf(buf, "451 Requested action aborted: local error in processing. %s\r\n", str);
    break;
  case 500:
    sprintf(buf, "500 Syntax error, command unrecognized. %s\r\n", str);
    break;
  case 501:
    sprintf(buf, "501 Syntax error in parameters or arguments. %s\r\n", str);
    break;
  case 504:
    sprintf(buf, "504 Command not implemented for that parameter. %s\r\n", str);
    break;
  case 530:
    sprintf(buf, "530 Not logged in. %s\r\n", str);
    break;
  case 550:
    sprintf(buf, "550 Requested action not taken.File unavailable (e.g., file not found, no access). %s\r\n", str);
    break;
  default:
    sprintf(buf, "%s", str);
    break;
  }
  strcasecmp(buf, str);
  strcasecmp(buf, "\r\n");
  send_str(clientfd, buf);
  printf("<-- %s\n", buf);
}

// Check if the client is logged in or not.
int pre_cmd_check(int clientfd, int isLogedin, int count, int expect_count)
{
  if (isLogedin == 0)
  {
    send_msg(clientfd, 530, "");
    return -1;
  }

  if (count != expect_count)
  {
    send_msg(clientfd, 501, "");
    return -1;
  }
  return isLogedin;
}

// Handling the CWD command.
void cwd_handle(int clientfd, char *path)
{
  regex_t regex;
  int return_value;
  return_value = regcomp(&regex, "^(\\.\\.|\\.).*(\\.\\.)?.*", REG_EXTENDED | REG_NOSUB);
  if (regexec(&regex, path, 0, NULL, 0) == 0)
  {
    send_msg(clientfd, 550, "");
  }
  else if (chdir(path) == 0)
  {
    send_msg(clientfd, 250, "");
  }
  else
  {
    send_msg(clientfd, 550, "");
  }
}

// Remove CRLF.
void trim_str(char *buf)
{
  if (buf != NULL)
  {
    buf[strcspn(buf, "\n")] = 0;
    buf[strcspn(buf, "\r")] = 0;
  }
}

// Handling different type of command.
int type_handle(int clentfd, char *code)
{
  if (strcasecmp(code, "A") == 0 || strcasecmp(code, "I") == 0)
  {
    send_msg(clentfd, 200, "");
    return 0;
  }
  else if (strcasecmp(code, "E") == 0 || strcasecmp(code, "L") == 0)
  {
    send_msg(clentfd, 504, "");
  }
  else
  {
    send_msg(clentfd, 501, "");
  }
  return 1;
}

// Creating new socket with given port number.
int new_socket(int port)
{
  int socket_fd = socket(PF_INET, SOCK_STREAM, 0);
  struct sockaddr_in address;
  bzero(&address, sizeof(struct sockaddr_in));

  // Find the next available ip
  struct ifaddrs *ifap, *ifa;
  struct sockaddr_in *sa;

  // Finding any availiable IP addr.
  getifaddrs(&ifap);
  for (ifa = ifap; ifa; ifa = ifa->ifa_next)
  {
    if (ifa->ifa_addr && ifa->ifa_addr->sa_family == AF_INET)
    {
      sa = (struct sockaddr_in *)ifa->ifa_addr;
    }
  }
  freeifaddrs(ifap);

  // Set the address is reusable
  int value = 1;
  if (setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &value, sizeof(int)) != 0)
  {
    perror("Failed to set the PASV socket option");
    return -1;
  }

  // Setting up the information needed for the address.
  address.sin_family = AF_INET;
  address.sin_port = htons(port);
  address.sin_addr.s_addr = sa->sin_addr.s_addr;

  if (bind(socket_fd, (struct sockaddr *)&address, sizeof(address)) < 0)
  {
    return -1;
  }

  if (listen(socket_fd, 1) < 0)
  {
    return -1;
  }

  return socket_fd;
}

// Accept the new data connection.
int accept_data_client(int psvsock_fd)
{

  // setup timeout on data connection
  struct timeval tv;
  fd_set readfs;
  int valopt;
  socklen_t lon;

  // 60 sec
  tv.tv_sec = TIMEOUT;
  tv.tv_usec = 0;

  FD_ZERO(&readfs);
  FD_SET(psvsock_fd, &readfs);

  int res = select(psvsock_fd + 1, &readfs, NULL, NULL, &tv);
  if (res < 0)
  {
    return -1;
  }
  else if (res > 0)
  {
    // Check the selcted socket has error
    lon = sizeof(int);
    if (getsockopt(psvsock_fd, SOL_SOCKET, SO_ERROR, (void *)(&valopt), &lon) < 0)
    {
      return -1;
        return -1; 
      return -1;
    }
    // Check the value returned.
    if (valopt)
    {
      return -1;
    }
    // Accept the client
    if (FD_ISSET(psvsock_fd, &readfs))
    {
      struct sockaddr_in retrAddress;
      socklen_t retrAddressLength = sizeof(struct sockaddr_in);
      return accept(psvsock_fd, (struct sockaddr *)&retrAddress, &retrAddressLength);
    }
  }
  return -1;
}

// RETR handler.
int retr_handle(int client_fd, int data_fd, char *filename)
{
  FILE *f = NULL;
  f = fopen(filename, "r");
  int ret = 1;
  if (f)
  {
    fseek(f, 0, SEEK_SET);
    char filebuf[SIZE + 1];
    int n, ret = 0;
    while ((n = fread(filebuf, 1, SIZE, f)) > 0)
    {
      int st = write(data_fd, filebuf, n);
      if (st < 0)
      {
        ret = -1;
      }
      if (n > st)
      {
        ret = -2;
      }
    }
  }
  else
  {
    ret = -1;
  }

  ret = fclose(f);

  if (ret == -1)
  {
    send_msg(client_fd, 426, "");
  }
  else if (ret == -2)
  {
    send_msg(client_fd, 426, "The short writes happend.");
  }
  else
  {
    send_msg(client_fd, 226, "");
  }
  return ret;
}

// Get the IP addres for input socket.
void getip(int sock, int *ip)
{
  socklen_t addr_size = sizeof(struct sockaddr_in);
  struct sockaddr_in addr;
  getsockname(sock, (struct sockaddr *)&addr, &addr_size);

  char *host = inet_ntoa(addr.sin_addr);
  sscanf(host, "%d.%d.%d.%d", &ip[0], &ip[1], &ip[2], &ip[3]);
}

// Get the port number for input socket.
void getport(int sock, int port)
{
  socklen_t addr_size = sizeof(struct sockaddr_in);
  struct sockaddr_in addr;
  getsockname(sock, (struct sockaddr *)&addr, &addr_size);

  port = ntohs(addr.sin_port);
}

void mode_handle(int client_fd, char *mode)
{
  if (strcasecmp(mode, "S") == 0)
  {
    send_msg(client_fd, 200, "");
  }
  else if (strcasecmp(mode, "B") == 0 || strcasecmp(mode, "C") == 0)
  {
    send_msg(client_fd, 504, "");
  }
  else
  {
    send_msg(client_fd, 501, "");
  }
}

void stru_handle(int client_fd, char *stru)
{
  if (strcasecmp(stru, "F") == 0)
  {
    send_msg(client_fd, 200, "");
  }
  else if (strcasecmp(stru, "R") == 0 || strcasecmp(stru, "P") == 0)
  {
    send_msg(client_fd, 504, "");
  }
  else
  {
    send_msg(client_fd, 501, "");
  }
}

void cdup_handle(int client_fd, const char *root_path)
{
  // Get the current path.
  char curPath[SIZE] = {0};
  getcwd(curPath, sizeof(curPath));
  printf("The current path is: %s\n", curPath);

  if (strcmp(root_path, curPath) == 0)
  {
    send_msg(client_fd, 550, curPath);
  }
  if (chdir("..") == 0)
  {
    send_msg(client_fd, 200, curPath);
  }
  else
  {
    send_msg(client_fd, 500, curPath);
  }
}
// Interact with client and handle all different commands.
void *interact(void *args)
{
  int data_client = -1;
  int data_ip[4];
  char *type_code = "A";
  int client_fd = *(int *)args, is_loggedin = 0, psv_socket = -1, is_pasv = -1;
  struct sockaddr_storage client_data_addr;
  char root_path[SIZE] = {0};
  getcwd(root_path, SIZE);
  send_msg(client_fd, 220, "");
  char buffer[1024];

  while (true)
  {
    bzero(buffer, 1024);
    // Receive the client message
    ssize_t length = recv(client_fd, buffer, 1024, 0);
    if (length < 0)
    {
      perror("Failed to read from the socket");
      break;
    }
    if (length == 0)
    {
      printf("EOF\n");
      break;
    }
    trim_str(buffer);
    printf("%s -->\n", buffer);

    int count = 0;
    char *ptr = buffer;
    while ((ptr = strchr(ptr, ' ')) != NULL)
    {
      count++;
      ptr++;
    }

    char *cmd = strtok(buffer, " ");
    trim_str(cmd);

    char *arg = strtok(NULL, " ");
    trim_str(arg);

    if (strcasecmp(cmd, "USER") == 0)
    {
      // Handle USER command.
      if (is_loggedin)
      {
        send_msg(client_fd, 220, "");
        continue;
      }

      if (count != 1)
      {
        send_msg(client_fd, 501, "");
        continue;
      }

      // Handling client called "cs317".
      if (strcmp(arg, "cs317") == 0)
      {
        send_msg(client_fd, 230, "");
        is_loggedin = 1;
      }
      else
      {
        send_msg(client_fd, 530, "Username is incorrect.");
      }
    }
    else if (strcasecmp(cmd, "QUIT") == 0)
    {
      // Handle QUIT command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 0) == 0)
      {
        continue;
      }
      send_msg(client_fd, 221, "");
      break;
    }
    else if (strcasecmp(cmd, "CWD") == 0)
    {
      // Handle CWD command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 1) == 0)
      {
        continue;
      }
      cwd_handle(client_fd, arg);
    }
    else if (strcasecmp(cmd, "CDUP") == 0)
    {
      // Handle CDUP command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 0) == 0)
      {
        continue;
      }
      cdup_handle(client_fd, root_path);
    }
    else if (strcasecmp(cmd, "TYPE") == 0)
    {
      // Handle TYPE command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 1) == 0)
      {
        continue;
      }

      if (type_handle(client_fd, arg) == 0)
      {
        type_code = arg;
      }
    }
    else if (strcasecmp(cmd, "MODE") == 0)
    {
      // Handle MODE command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 1) == 0)
      {
        continue;
      }
      mode_handle(client_fd, arg);
    }
    else if (strcasecmp(cmd, "STRU") == 0)
    {
      // Handle STRU command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 1) == 0)
      {
        continue;
      }
      stru_handle(client_fd, arg);
    }
    else if (strcasecmp(cmd, "RETR") == 0)
    {
      // Handle RETR command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 1) == 0)
      {
        continue;
      }

      if (is_pasv)
      {
        data_client = accept_data_client(psv_socket);
      }
      else
      {
        send_msg(client_fd, 425, "");
        continue;
      }

      if (data_client < 0)
      {
        send_msg(client_fd, 425, "");
        continue;
      }

      send_msg(client_fd, 150, "");

      // Call the handler for RETR command.
      int ret = retr_handle(client_fd, data_client, arg);
      close(data_client);
      close(psv_socket);
    }
    else if (strcasecmp(cmd, "PASV") == 0)
    {
      // Handle PASV command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 0) == 0)
      {
        continue;
      }

      // Close the passive socket if it is active
      if (psv_socket >= 0)
      {
        close(psv_socket);
      }

      // Set up a random port number for data connection.
      int data_port = (rand() % (65535 - 1024) + 1024);
      psv_socket = new_socket(data_port);

      if (psv_socket < 0)
      {
        send_msg(client_fd, 500, "");
      }
      else
      {
        is_pasv = 1;
        char sendbuf[1024];
        getip(psv_socket, data_ip);
        sprintf(sendbuf, "227 Entering Passive Mode (%d,%d,%d,%d,%d,%d)\n",
                (int)data_ip[0] & 0xff,
                (int)data_ip[1] & 0xff,
                (int)data_ip[2] & 0xff,
                (int)data_ip[3] & 0xff,
                data_port / 256,
                data_port % 256);
        send_str(client_fd, sendbuf);
      }
    }
    else if (strcasecmp(cmd, "NLST") == 0)
    {
      // Handle NLST command.
      if (pre_cmd_check(client_fd, is_loggedin, count, 0) == 0)
      {
        continue;
      }

      if (is_pasv)
      {
        data_client = accept_data_client(psv_socket);
      }

      // Start the transferring
      send_msg(client_fd, 150, "");

      int ret = listFiles(data_client, ".");

      // Handle erros
      if (ret == -1)
      {
        send_msg(client_fd, 451, "");
      }
      else if (ret == -2)
      {
        send_msg(client_fd, 450, "");
      }
      else
      {
        send_msg(client_fd, 226, "");
      }
      close(data_client);
      close(psv_socket);
    }
    else
    {
      send_msg(client_fd, 500, "");
    }
  }
  close(client_fd);
  return NULL;
}

int main(int argc, char *argv[])
{
  // Check the command line arguments
  if (argc != 2)
  {
    usage(argv[0]);
    return -1;
  }
  int port = (int)strtol(argv[1], NULL, 10);

  // Create a TCP socket
  int socketd = socket(PF_INET, SOCK_STREAM, 0);
  if (socketd < 0)
  {
    perror("Failed to create the socket.");
    exit(-1);
  }
  // Reuse the address
  int value = 1;
  if (setsockopt(socketd, SOL_SOCKET, SO_REUSEADDR, &value, sizeof(int)) != 0)
  {
    perror("Failed to set the socket option");
    exit(-1);
  }
  // Bind the socket to a port
  struct sockaddr_in address;
  bzero(&address, sizeof(struct sockaddr_in));
  address.sin_family = AF_INET;
  address.sin_port = htons(port);
  address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
  if (bind(socketd, (const struct sockaddr *)&address, sizeof(struct sockaddr_in)) != 0)
  {
    perror("Failed to bind the socket");
    exit(-1);
  }
  // Set the socket to listen for connections
  if (listen(socketd, 10) != 0)
  {
    perror("Failed to listen for connections");
    exit(-1);
  }
  while (true)
  {
    // Accept the connection
    struct sockaddr_in clientAddress;
    socklen_t clientAddressLength = sizeof(struct sockaddr_in);
    printf("Waiting for incomming connections...\n");
    int clientd = accept(socketd, (struct sockaddr *)&clientAddress, &clientAddressLength);
    if (clientd < 0)
    {
      perror("Failed to accept the client connection");
      continue;
    }
    printf("Accepted the client connection from %s:%d.\n", inet_ntoa(clientAddress.sin_addr), ntohs(clientAddress.sin_port));
    // Create a separate thread to interact with the client
    pthread_t thread;
    if (pthread_create(&thread, NULL, interact, &clientd) != 0)
    {
      perror("Failed to create the thread");
      continue;
    }
    // The main thread just waits until the interaction is done
    pthread_join(thread, NULL);
    printf("Interaction thread has finished.\n");
  }
  return 0;
}

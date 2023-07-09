/* Abhimanyu Kumar HW1a
 *  The purpose for this program is creating a child  process, executing a new
 *  program in the child process
 */

#include <sys/types.h> // for fork, wait
#include <sys/wait.h>  // for wait
#include <unistd.h>    // for fork, pipe, dup, close
#include <stdio.h>     // for NULL, perror
#include <stdlib.h>    // for exit

#include <iostream> // for cout

using namespace std;

enum
{
  READ = 0,
  WRITE = 1
};

int main(int argc, char **argv)
{
  int fds[2][2];
  // cout << fds[0] << endl;
  int pid;

  if (argc != 2)
  {
    cerr << "Usage: processes command" << endl;
    exit(-1);
  }

  // fork a child
  if ((pid = fork()) < 0)
  { // 1st
    perror("fork error");
  }
  else if (pid == 0)
  {
    pipe(fds[0]);           // create a pipe using fds[0]
    if ((pid = fork()) < 0) // fork a grand-child
    {                       // 2nd
      perror("fork error");
    }
    else if (pid == 0)
    {
      pipe(fds[1]);           // create a pipe using fds[1]
      if ((pid = fork()) < 0) // fork a great-grand-child
      {                       // 2nd
        perror("fork error");
      }
      else if (pid == 0)
      {
        // great grandchild
        dup2(fds[1][WRITE], STDOUT_FILENO);
        close(fds[1][READ]);
        execlp("ps", "ps", "-A", NULL); // execute "ps"
        exit(0);
      }
      else
      {
        // grandchild
        wait(NULL);
        dup2(fds[1][READ], STDIN_FILENO);
        dup2(fds[0][WRITE], STDOUT_FILENO);
        close(fds[1][WRITE]);
        close(fds[0][READ]);
        execlp("grep", "grep", argv[1], NULL); // execute "grep"
        exit(0);
      }
    }
    else
    {
      // child process
      wait(NULL);
      dup2(fds[0][READ], STDIN_FILENO);
      close(fds[0][WRITE]);
      execlp("wc", "wc", "-l", NULL); // execute "wc"

      exit(0);
    }
  }
  else
  {
    // parent process
    wait(NULL);
    cout << "commands completed" << endl;
  }
}

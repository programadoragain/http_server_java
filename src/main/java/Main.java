import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private static final ExecutorService EXECUTOR_SERVICE= Executors.newFixedThreadPool(5);
  public static void main(String[] args) {  
    
    Integer port= 4221;
    
    String directoryPath;

    if ((args.length == 2) && 
        (args[0].equalsIgnoreCase("--directory"))) directoryPath= args[1];
  
    try {

      @SuppressWarnings("resource")
      ServerSocket serverSocket= new ServerSocket(port);
      Socket clientSocket= null;

      serverSocket.setReuseAddress(true);

      while (true) {
        clientSocket = serverSocket.accept();
        EXECUTOR_SERVICE.submit(new ClientHandler(clientSocket, "C:\\"));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
      
}

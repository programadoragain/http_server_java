import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  private static final String HTTP_VERSION = "HTTP/1.1";
  private static final String STATUS_OK = "200 OK";
  private static final String STATUS_BAD = "404 Not Found";
  private static final String CRLF = "\r\n";
  private static final String GET_METHOD = "GET";

  public static void main(String[] args) throws IOException {

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {

      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("Accepted new connection");

      BufferedReader bufferIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String lineInput;

      while ((lineInput = bufferIn.readLine()) != null) {
        String[] headersLine = lineInput.split(" ");
        String serverResponse;

        if ((headersLine[0].equals(GET_METHOD)) &&
            (headersLine[1].startsWith("/")) &&
            (headersLine[1].length() == 1) &&
            (headersLine[2].equals(HTTP_VERSION))) {
          serverResponse = HTTP_VERSION + " " + STATUS_OK + CRLF + CRLF;
          clientSocket.getOutputStream().write(serverResponse.getBytes());

        } else if ((headersLine[0].equals(GET_METHOD)) &&
                   (headersLine[1].startsWith("/echo/")) &&
                   (headersLine[2].equals(HTTP_VERSION))) {
            clientSocket.getOutputStream().write(formatResponseEchoOK(headersLine[1]).getBytes());
        
          } else if ((headersLine[0].equals(GET_METHOD)) &&
                   (headersLine[1].startsWith("/user-agent")) &&
                   (headersLine[2].equals(HTTP_VERSION))) {
            while ((lineInput = bufferIn.readLine()) != null) {
              if (lineInput.startsWith("User-Agent: ")) {
                clientSocket.getOutputStream().write(formatResponseUserAgentOK(lineInput.substring(12,lineInput.length())).getBytes());
                break;
              }
            }
          
            } else {
              serverResponse = HTTP_VERSION + " " + STATUS_BAD + CRLF + CRLF;
              clientSocket.getOutputStream().write(serverResponse.getBytes());
              break;
            }
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (clientSocket.isConnected())
        clientSocket.close();
    }

  }

  private static String formatResponseEchoOK(String headerRoute) {
    String subRoute = headerRoute.substring(6, headerRoute.length());

    String formatResponse = HTTP_VERSION + " " + STATUS_OK + CRLF
        + "Content-Type: text/plain" + CRLF
        + "Content-Length: " + subRoute.length() + CRLF
        + CRLF
        + subRoute + CRLF + CRLF;

    return formatResponse;
  }

  private static String formatResponseUserAgentOK(String headerUserAgent) {
    String formatResponse = HTTP_VERSION + " " + STATUS_OK + CRLF
        + "Content-Type: text/plain" + CRLF
        + "Content-Length: " + headerUserAgent.length() + CRLF
        + CRLF
        + headerUserAgent + CRLF + CRLF;

    return formatResponse;
  }

}

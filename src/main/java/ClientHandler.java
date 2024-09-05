import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private String directoryPath;

    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String STATUS_200 = "200 OK";
    private static final String STATUS_201 = "201 CREATED";
    private static final String STATUS_404 = "404 Not Found";
    private static final String CRLF = "\r\n";
    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";

    public ClientHandler(Socket clientSocket, String directoryPath) {
        this.clientSocket = clientSocket;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
        try {
            // System.out.println("Accepted new connection");

            BufferedReader bufferIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String lineInput;

            while ((lineInput = bufferIn.readLine()) != null) {
                String[] headersLine = lineInput.split(" ");

                if (!(checkRequestStruct(headersLine)))
                    break;

                if (isDirectoryRequest(headersLine)) {

                    clientSocket.getOutputStream().write(formatResponse200().getBytes());

                } else if (isEchoRequest(headersLine)) {

                    String echoContent = headersLine[1].substring("/echo/".length());

                    clientSocket.getOutputStream().write(formatResponse200WithBodyOk(echoContent).getBytes());

                } else if (isUserAgentRequest(headersLine)) {

                    while ((lineInput = bufferIn.readLine()) != null) {
                        if (lineInput.startsWith("User-Agent: ")) {
                            String userAgentContent = lineInput.substring("User-Agent: ".length());
                            clientSocket.getOutputStream().write(formatResponse200WithBodyOk(userAgentContent).getBytes());
                        }
                    }

                } else if (isGetFileRequest(headersLine)) {

                    String fileName = extractFileName(headersLine[1]);

                    if (directoryPath != null) {
                        File file = new File(directoryPath + "/" + fileName);
                        Path path = file.toPath();

                        if (Files.exists(path)) {
                            clientSocket.getOutputStream().write(formatResponseFileOK(path).getBytes());
                        } else {
                            clientSocket.getOutputStream().write(formatResponse404().getBytes());
                        }
                    }

                } else if (isPostFileRequest(headersLine)) {

                    String createfileName = extractFileName(headersLine[1]);

                    if (directoryPath != null) {
                        File file = new File(directoryPath + createfileName);

                        while ((lineInput = bufferIn.readLine()) != null) {
                            
                            if (lineInput.isEmpty()) {
                                
                                String body = bufferIn.readLine();
         
                                FileWriter writer = new FileWriter(file);
                                writer.write(body);
                                writer.close();
                                clientSocket.getOutputStream().write(formatResponse201().getBytes());
                                break;
                            }
                        }
                    }

                } else {
                    clientSocket.getOutputStream().write(formatResponse404().getBytes());
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());

        } finally {
            if (clientSocket.isConnected())
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }
    }

    private String extractFileName(String subPath) {
        return subPath.substring("/files/".length());
    }

    private boolean checkRequestStruct(String[] headersLine) {
        return ((headersLine.length == 3) &&
                (headersLine[0].equals(GET_METHOD)) &&
                (headersLine[2].equals(HTTP_VERSION)) ||
                (headersLine[0].equals(POST_METHOD))) ? true : false;
    }

    private boolean isDirectoryRequest(String[] headersLine) {
        return ((headersLine[1].startsWith("/")) && (headersLine[1].length() == 1)) ? true : false;
    }

    private boolean isEchoRequest(String[] headersLine) {
        return (headersLine[1].startsWith("/echo/")) ? true : false;
    }

    private boolean isUserAgentRequest(String[] headersLine) {
        return (headersLine[1].startsWith("/user-agent")) ? true : false;
    }

    private boolean isGetFileRequest(String[] headersLine) {
        return (headersLine[0].equals(GET_METHOD) &&
                (headersLine[1].startsWith("/files/"))) ? true : false;
    }

    private boolean isPostFileRequest(String[] headersLine) {
        return (headersLine[0].equals(POST_METHOD) &&
                (headersLine[1].startsWith("/files/"))) ? true : false;
    }

    private static String formatResponse200() {
        return HTTP_VERSION + " " + STATUS_200 + CRLF + CRLF;
    }

    private static String formatResponse201() {
        return HTTP_VERSION + " " + STATUS_201 + CRLF + CRLF;
    }

    private static String formatResponse404() {
        return HTTP_VERSION + " " + STATUS_404 + CRLF + CRLF;
    }

    private static String formatResponse200WithBodyOk(String content) {
        String formatResponse = HTTP_VERSION + " " + STATUS_200 + CRLF
                + "Content-Type: text/plain" + CRLF
                + "Content-Length: " + content.length() + CRLF
                + CRLF
                + content + CRLF + CRLF;

        return formatResponse;
    }

    private static String formatResponseFileOK(Path filePath) throws IOException {
        String formatResponse = HTTP_VERSION + " " + STATUS_200 + CRLF
                + "Content-Type: application/octet-stream" + CRLF
                + "Content-Length: " + String.valueOf(Files.size(filePath)) + CRLF
                + CRLF
                + Files.readString(filePath) + CRLF + CRLF;

        return formatResponse;
    }

}

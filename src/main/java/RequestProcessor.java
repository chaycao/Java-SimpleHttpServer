import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by chaycao on 2017/7/4.
 */
public class RequestProcessor implements Runnable{
    private static List pool = new LinkedList<Socket>();
    private File documentRootDirectory;
    private String indexFileName = "index.html";

    public RequestProcessor(File documentRootDirectory, String indexFileName){
        if(documentRootDirectory.isFile()){
            throw new IllegalArgumentException();
        }
        this.documentRootDirectory = documentRootDirectory;
        try {
            this.documentRootDirectory = documentRootDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (indexFileName != null){
            this.indexFileName = indexFileName;
        }
    }

    /**
     * 处理新的请求，将请求加到pool的末尾
     * 并通知所有的线程，新请求的到来
     * @param request
     */
    public static void processRequest(Socket request){
        synchronized (pool){
            pool.add(pool.size(), request);
            pool.notify();  //疑问！！一定要用notifyAll()吗？是否可以notify，因为这里只有一个请求的加入
        }
    }

    /**
     * 若连接池中不为空，则取出一个请求做处理
     * 否则一直等待
     */
    public void run() {
        // 安全性检测
        String root = documentRootDirectory.getPath();

        while (true){
            Socket connection;
            synchronized (pool){
                while (pool.isEmpty()){
                    try {
                        pool.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                connection = (Socket)pool.remove(0);
            }

            try {
                String fileName;
                String contentType;
                OutputStream raw = new BufferedOutputStream(connection.getOutputStream());
                Writer out = new OutputStreamWriter(raw);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String get = in.readLine();
                // 记录日志
                System.out.println(get + "  " + Thread.currentThread().getId());

                StringTokenizer st = new StringTokenizer(get);
                String method = st.nextToken();
                String version = "";
                if(method.equals("GET")){
                    fileName = st.nextToken();
                    if(fileName.endsWith("/")){
                        fileName += indexFileName;
                    }
                    contentType = guessContentTypeFromName(fileName);
                    if(st.hasMoreTokens()){
                        version = st.nextToken();
                    }

                    File theFile = new File(documentRootDirectory, fileName.substring(1,fileName.length()));
                    if(theFile.canRead() && theFile.getCanonicalPath().startsWith(root)){
                        DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(theFile)));
                        byte[] theData = new byte[(int)theFile.length()];
                        fis.readFully(theData);
                        fis.close();
                        if (version.startsWith("HTTP")){
                            out.write("HTTP/1.0 200 OK\r\n");
                            Date now = new Date();
                            out.write("Date: " + now + "\r\n");
                            out.write("Server: JHTTP 1.0\r\n" );
                            out.write("Content-length: " + theData.length + "\r\n");
                            out.write("Content-type: " + contentType +"\r\n\r\n");
                            out.flush();
                        }
                        raw.write(theData);
                        raw.flush();
                    }else {
                        if (version.startsWith("HTTP")) {
                            out.write("HTTP/1.0 404 File Not Found\r\n");
                            Date now=new Date();
                            out.write("Date: "+now+"\r\n");
                            out.write("Server: JHTTP 1.0\r\n");
                            out.write("Content-Type: text/html\r\n\r\n");
                            out.flush();
                        }
                        out.write("<HTML>\r\n");
                        out.write("<HEAD><TITLE>File Not Found</TITLE></HRAD>\r\n");
                        out.write("<BODY>\r\n");
                        out.write("<H1>HTTP Error 404: File Not Found</H1>");
                        out.write("</BODY></HTML>\r\n");
                        out.flush();
                    }
                }else {//方法不等于GET
                    if (version.startsWith("HTTP")) {
                        out.write("HTTP/1.0 501 Not Implemented\r\n");
                        Date now=new Date();
                        out.write("Date: "+now+"\r\n");
                        out.write("Server: JHTTP 1.0\r\n");
                        out.write("Content-Type: text/html\r\n\r\n");
                        out.flush();
                    }
                    out.write("<HTML>\r\n");
                    out.write("<HEAD><TITLE>Not Implemented</TITLE></HRAD>\r\n");
                    out.write("<BODY>\r\n");
                    out.write("<H1>HTTP Error 501: Not Implemented</H1>");
                    out.write("</BODY></HTML>\r\n");
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static String guessContentTypeFromName(String name){
        if(name.endsWith(".html") || name.endsWith(".htm"))
            return "text/html";
        else if (name.endsWith(".txt") || name.endsWith(".java"))
            return "text/plain";
        else if (name.endsWith(".gif"))
            return "image/gif";
        else if (name.endsWith(".class"))
            return "application/octet-stream";
        else if (name.endsWith(".jpg")||name.endsWith(".jpeg")) {
            return "image/jpeg";
        }else {
            return "text/plain";
        }
    }
}

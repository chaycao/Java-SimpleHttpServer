import java.io.*;
import java.net.*;

import org.omg.CORBA.Request;

/**
 * Created by chaycao on 2017/7/4.
 *
 * 提供完整的文档树，包括图片、applet、HTML、文本文件等
 * 关注GET请求
 * 将每个请求放入池中，有RequestProcessor类实例从池中
 * 移走并进行处理
 */
public class JHTTP extends Thread{
    private File documentRootDirectory;
    private String indexFileName = "index.html";
    private ServerSocket server;
    private int numThreads = 50;

    public JHTTP(File documentRootDirectory, int port, String indexFileName) throws IOException{
        if(!documentRootDirectory.isDirectory()){
            throw new IOException(documentRootDirectory + " does not exist as a directory");
        }
        this.documentRootDirectory = documentRootDirectory;
        this.indexFileName = indexFileName;
        this.server = new ServerSocket(port);
    }

    private JHTTP(File documentRootDirectory, int port) throws IOException{
        this(documentRootDirectory, port, "index.html");
    }

    public void run(){
        for(int i = 0; i < numThreads; i++){
            Thread t = new Thread(new RequestProcessor(documentRootDirectory, indexFileName));
            t.start();
        }
        System.out.println("Accepting connection on port "
                + server.getLocalPort());
        System.out.println("Document Root: " + documentRootDirectory);
        while (true) {
            try {
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        File docroot;
        try {
            docroot = new File(args[0]);
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Usage: java JHTTP docroot port indexfile");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
            if (port<0 || port>65535)
                port = 80;
        } catch (Exception e) {
            port = 9;
        }

        try {
            JHTTP webserver = new JHTTP(docroot, port);
            webserver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

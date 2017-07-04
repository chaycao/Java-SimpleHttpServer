import java.io.*;
import java.net.*;

/**
 * Created by chaycao on 2017/7/2.
 */

/**
 * 简单的单文件服务器
 * 功能：无论接受到何种请求，始终发送同一个文件。
 * 文件名、本地端口、内容编码方式从命令行读取。端口缺省为80，编码方式缺省为ASCII
 */
public class SingleFileHTTPServer extends Thread {

    private byte[] content;     //保存的文件内容
    private byte[] header;      //response的消息头
    private int port = 80;      //默认端口80

    private SingleFileHTTPServer(String data, String encoding,
                                 String MiMEType, int port) throws UnsupportedEncodingException {
        this(data.getBytes(encoding), encoding, MiMEType, port);
    }

    public SingleFileHTTPServer(byte[] data, String encoding, String MiMEType, int port) throws UnsupportedEncodingException {
        this.content = data;
        this.port = port;
        String header =
                "HTTP/1.0 200 OK\r\n" +
                "Server: OneFile 1.0\r\n" +
                "Content-length: " + this.content.length + "\r\n" +
                "Content-type: " + MiMEType + "\r\n\r\n";
        this.header = header.getBytes("ASCII");
    }

    /**
     * 1.打开Server，绑定端口
     * 2.死循环，接收请求，返回已经保存好的文件内容
     */
    public void run(){
        try {
            // 1.打开server，绑定到固定端口
            ServerSocket server = new ServerSocket(this.port);
            System.out.println("Accepting connections on port " + server.getLocalPort());
            System.out.println("Data to be sent: ");
            System.out.write(this.content);

            // 2.死循环，接收请求
            while (true) {
                Socket connection = null;
                try {
                    connection = server.accept();
                    OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    // 读取HTTP请求的第一行
                    StringBuffer request = new StringBuffer();
                    request.append(input.readLine());

                    // 如果检测是HTTP/1.0及以后的协议，按照规范，需要发送一个MIMIE首部
                    if(request.toString().indexOf("HTTP/") != -1){
                        out.write(this.header);
                    }
                    out.write(this.content);
                    out.flush();
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    connection.close();
                }
            }

        } catch (IOException e) {
            System.out.println("Could not start server. Port Occupied");
        }
    }

    /**
     *  对SingleFileHTTPServer初始化，启动服务实例
     * @param args：文件名；端口；编码
     */
    public static void main(String[] args) {
        try {
            // 1. 设置contentType，并读取文件
            String contentType = "text/plain";
            if(args[0].endsWith(".html") || args[0].endsWith(".htm"))
                contentType = "text/html";

            BufferedInputStream input = new BufferedInputStream(new FileInputStream(args[0]));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int copySize;
            while ((copySize = input.read(buffer)) > 0){
                out.write(buffer, 0, copySize);
            }
            byte[] data = out.toByteArray();


            // 2.设置监听端口
            int port;
            try {
                port = Integer.parseInt(args[1]);
                if(port < 1 || port > 65535){
                    port = 80;
                }
            }catch (Exception e){
                port = 80;
            }

            // 3.设置编码格式
            String encoding = "ASCII";
            if(args.length > 2){
                encoding = args[2];
            }

            // 4.启动服务器
            Thread t = new SingleFileHTTPServer(data, encoding, contentType, port);
            t.start();

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage:java SingleFileHTTPServer filename port encoding");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

import java.net.*;
import java.io.*;

/**
 * Created by chaycao on 2017/7/2.
 */
public class HttpServer implements Runnable{
    private ServerSocket serverSocket;

    public HttpServer(int port){
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("端口被占用！请尝试别的端口");
            e.printStackTrace();
        }

    }

    public void run(){
        System.out.println("服务器已启动...");
        while (true){
            try {
                Socket socket = serverSocket.accept();
                System.out.println("收到请求来自：" + socket.getInetAddress());
                new Thread(new Handler(socket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动HttpServer服务
     * @param args
     * args[0] : port
     */
    public static void main(String[] args) {

        HttpServer httpServer = new HttpServer(Integer.valueOf(8088));
        new Thread(httpServer).start();
    }
}

class Handler implements Runnable{
    private Socket socket;

    public Handler(Socket socket){
        this.socket = socket;
    }

    public void run() {
        handle(socket);
    }

    /**
     * 处理HTTP请求
     * @param socket
     */
    private void handle(Socket socket) {
        // 1.从socket中读取HTTP请求
        InputStream input = null;
        try {
            input = socket.getInputStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader(input));
            String s;
            StringBuilder sb = new StringBuilder();
            while ((s = reader.readLine())!=null){
                sb.append(s+"\n");
                System.out.println("test");
            }
            String request = sb.toString();
            System.out.println(request);

            // 2.写响应
            OutputStream output = socket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
            writer.write("Success!");
            writer.flush();
            // 3.关闭
            input.close();
            output.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
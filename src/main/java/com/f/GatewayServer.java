package com.f;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
// https://juejin.cn/post/6986652925963534350

public class GatewayServer {
    private static final int PORT = 8899;

    public static void main(String[] args) {
        try {
            // 创建ServerSocket对象，用于监听指定端口
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("网关服务器已启动，正在监听端口 " + PORT);

            // 创建线程池，例如设置线程池大小为10（可根据实际情况调整）
            ExecutorService executorService = Executors.newFixedThreadPool(10);

            // 进入循环，不断接收客户端连接
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("收到客户端连接：" + clientSocket.getInetAddress());

                // 处理客户端连接，这里可以创建一个新的线程来处理，以实现并发处理，（优化成线程池）
                // new Thread(() -> handleClientConnection(clientSocket)).start();
                executorService.submit(() -> handleClientConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        // 具体的请求处理逻辑
        try {
            // 从客户端Socket获取输入流，并创建BufferedReader用于读取数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
            // 这里可以继续读取请求的其他部分，如请求头、请求体等，根据具体需求而定
            String httpLine = reader.readLine();
            if (!isValidHttp(httpLine)) {
                throw new IllegalArgumentException("无效的HTTP请求：" + httpLine);
            }
            // 暂存后续步骤要使用的请求信息
            StringBuffer requestBuffer = new StringBuffer();
            requestBuffer.append(httpLine);
            // 读取请求的其他部分（如请求头），这里简单示例读取到遇到空行结束
            String line;
            while ((line = reader.readLine()) != null && !line.equals("")) {
                /*if (!isValidRequestHeaderLine(line)) {
                    throw new IllegalArgumentException("无效的请求头格式：" + line);
                }*/
                requestBuffer.append("\n").append(line);
            }
            String request = requestBuffer.toString();
            System.out.println(request);
            // 请求转发
            // 连接后端服务器
            Socket backendSocket = new Socket("127.0.0.1", 8080);
            System.out.println("已连接到后端服务器");
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(backendSocket.getOutputStream(),"UTF-8"), true);
            // 写入客户端请求到后端服务器
            writer.write(request);
            writer.flush();

            // 接收后端服务器响应
            BufferedReader backendReader = new BufferedReader(new InputStreamReader(backendSocket.getInputStream(),"UTF-8"));

            // 暂存后端服务器响应信息
            StringBuffer responseBuffer = new StringBuffer();

            // 逐行读取后端服务器响应并暂存
            String respline;
            while ((respline = backendReader.readLine()) != null) {
                responseBuffer.append("\n").append(respline);
            }

            // 获取完整的响应信息
            String response = responseBuffer.toString();
            System.out.println("response:" + response);

            // 将后端服务器响应返回给客户端
            // 从客户端Socket获取输出流，并创建PrintWriter用于写入数据
            PrintWriter clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true);

            // 写入后端服务器响应到客户端
            clientWriter.write(response);
            clientWriter.flush();

            // 关闭相关Socket连接，这里可以根据需要进行优化，比如在适当的时候关闭
            clientSocket.close();
            backendSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidHttp(String requestLine) {
        Pattern pattern = Pattern.compile("^[A-Z]+\\s+\\S+\\s+HTTP/\\d+\\.\\d+$");
        return pattern.matcher(requestLine).matches();
    }
    // 以下是验证请求头每行格式的方法
    private static boolean isValidRequestHeaderLine(String line) {
        Pattern pattern = Pattern.compile("^\\w+:\\s+.*$");
        return pattern.matcher(line).matches();
    }

}

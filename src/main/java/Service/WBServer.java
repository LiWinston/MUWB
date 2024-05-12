package Service;

import GUI.WhiteBoard;
import WBSYS.Properties;
import io.grpc.*;
import whiteboard.WhiteBoardSecuredServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static WBSYS.Properties.isValidPort;


public class WBServer {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";
    private static final Logger logger = Logger.getLogger(WBServer.class.getName());
    private static String port;
    private final WhiteBoard wb;
    private Server server;

    WBServer(WhiteBoard wb) {
        this.wb = wb;
    }

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {

            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
                System.out.println("valid port range : 1024-65535");
            } else {
                port = args[1];
                String name = (args.length == 3) ? args[2] : DEFAULT_WHITEBOARD_NAME;
                String IpAddress = args[0];


                try {
                    InetAddress inetAddress = InetAddress.getByName(IpAddress);
                    String Ip = inetAddress.getHostAddress();
                    WhiteBoard wb = new WhiteBoard();
                    final WBServer server = new WBServer(wb);

                    // 开启新线程启动服务器
                    new Thread(() -> {
                        try {
                            server.start();
                            server.blockUntilShutdown();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();


                    logger.info("IP address set for sv: " + Ip);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(Ip, Integer.parseInt(port)).usePlaintext().build();
                    wb.registerManager(Ip, port, name, channel);
                    //public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName)
//                    new ManagerGUI(wb, Ip, port, name);
                    wb.setServerStub(WhiteBoardServiceGrpc.newStub(channel));
                    wb.setManagerSecuredStub(WhiteBoardSecuredServiceGrpc.newStub(channel));
                    wb.pushMessage(Properties.managerMessage("White Board Setup, Server IP: " + Ip + " Port: " + port + " Board Name: " + name));
                    logger.info("Manager GUI initialized, welcome message sent");
                } catch (IOException e) {
                    logger.severe("IOException: Server failed to start");
                }
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
        }
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(Integer.parseInt(port)).
                addService(new WhiteBoardServiceImpl(wb, logger)).
                addService(new WhiteBoardClientImpl(wb, logger)).
                addService(ServerInterceptors.intercept(
                        new WhiteBoardSecuredServiceImpl(wb, logger),
                        new JwtServerInterceptor())).
                keepAliveTime(15, TimeUnit.MINUTES).
                permitKeepAliveWithoutCalls(true).
                maxConnectionAgeGrace(3, TimeUnit.SECONDS). // 允许3s的宽限期完成正在进行的RPC
        build().start();
        logger.info("grpc Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                WBServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            wb.managerClose();
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}

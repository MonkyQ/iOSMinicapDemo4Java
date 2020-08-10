import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author monky
 * IOSMinica端口转发
 */
public class IOSMinicapClient {
    private static int connectNum = 30;
    private static Socket mSocket = null;
    private static ServerSocket mServer = null;
    public static final String IOS_MINICAP_SOCKET = "satIosMinicapSocket";
    public static Map<String, Object> IOS_MINICAP_SOCKET_MAP = new HashMap<String, Object>();

    public void iosMinicapStart() {
            String portProcess=null;
            try {
                portProcess=IOSMinicapServer.initIOSMinicapRun();
                if(portProcess!=null){
                    if(null != IOS_MINICAP_SOCKET_MAP.get(IOS_MINICAP_SOCKET) && !IOS_MINICAP_SOCKET_MAP.get(IOS_MINICAP_SOCKET).equals("")) {
                        Socket minicapSocket = (Socket)IOS_MINICAP_SOCKET_MAP.get(IOS_MINICAP_SOCKET);
                        minicapSocket.close();
                    }
                    iosMinicapClose();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            connectNum = 30;
            String path = System.getProperty("user.dir");
            String minicapCmd = path + "/ios-minicap" + "/run.sh ";
            System.out.println("starting ios-minicap .....");
            /** 如果有ios设备则正常启动，否则提示用户未连接设备并取消启动appium，add by chenlg，20170206 */
            try {
                Runtime.getRuntime().exec("osascript -e 'tell application \"Terminal\" to close (every window whose name contains \"ios_minicap\")' &");
                Thread.sleep(500);
                Runtime.getRuntime().exec("xattr -c "+minicapCmd);
                Thread.sleep(2000);
                //为sh文件赋权
                Runtime.getRuntime().exec("chmod 777 "+minicapCmd);
                Runtime.getRuntime().exec("/usr/bin/open -a Terminal "+minicapCmd);
                System.out.println("exec:/usr/bin/open -a Terminal "+minicapCmd);

                Thread.sleep(2000);
                int timeout = 40000;
                while (timeout > 0) {
                    try {
                        portProcess = IOSMinicapServer.initIOSMinicapRun();
                        if(portProcess!=null){
                            Thread.sleep(1000);
                            break;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to start ios-minicap server.minicapCmd=<" + "lsof -i tcp:12345" + ">");
                    }
                    timeout = timeout - 2000;
                    Thread.sleep(3000);
                }
                if(portProcess == null){
                    throw new RuntimeException("Failed to start ios-minicap, please make sure ios device connected.");
                }
                Thread.sleep(3000);

                new Thread(new Runnable() {
                    private Socket socket;

                    @Override
                    public void run() {
                        iosMinicapSocketStart(socket);
                    }
                }).start();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("Failed to start minicap server.minicapCmd=<" + "/usr/bin/open -a Terminal "+minicapCmd + ">");
            }
        }

    public static void iosMinicapSocketStart(Socket socket) {
        try {
            if (connectNum <= 0 ) {
                return;
            }
            connectNum--;
            System.out.println("checking minicap waiting...:"+connectNum);
            socket = new Socket("127.0.0.1", 12345);
            System.out.println("exec:127.0.0.1:12345 socket 连接成功");
            mServer = new ServerSocket(23456);
            while (true) {
                mSocket = mServer.accept();
                try{
                    socket.sendUrgentData(0xFF);
                    Thread.sleep(50);
                    socket.sendUrgentData(0xFF);
                }catch(Exception ex){
                    mServer.close();
                    mSocket.close();
                }
                if (!mSocket.isClosed()) {
                    System.out.println("exec:127.0.0.1:23456 socket 连接成功");
                    try {
                        while (true) {
                            InputStream input = socket.getInputStream();
                            int len = 0;
                            while (len == 0) {
                                Thread.sleep(5);
                                len = input.available();
                            }
                            byte[] cbuf=new byte[len];
                            input.read(cbuf);
                            mSocket.getOutputStream().write(cbuf);
                        }
                    } catch (Exception e) {
                        System.out.println("exec:127.0.0.1:23456 socket 关闭连接"+e);
                    }
                }
            }
        } catch (Exception e) {
//				e.printStackTrace();
            try {
                Thread.sleep(2000);
					if (null != socket) {
						socket.close();
					}
					if (null != mSocket) {
						mSocket.close();
					}
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            iosMinicapSocketStart(socket);
        }
    }


    /**
     * 关闭ios-minicap服务
     */
    public static void iosMinicapClose()
    {
            String path = System.getProperty("user.dir");
            String exitMinicapCmd = path+"/exitMinicap.sh ";
            try {
                if (mServer != null) {
                    mServer.close();
                    mServer = null;
                }
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                Runtime.getRuntime().exec(exitMinicapCmd);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("Failed to close ios-minicap server.exitMinicapCmd=<" + exitMinicapCmd + ">");
            }
        }

    public static void main(String[] args) {
        IOSMinicapClient client = new IOSMinicapClient();
        client.iosMinicapStart();
    }
}

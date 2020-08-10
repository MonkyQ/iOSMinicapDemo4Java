import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author monky
 * 单线实现iOSMinicap收发
 */
public class IOSMinicapMachine {
    private int readBannerBytes = 0;
    private int bannerLength = 2;
    private int readFrameBytes = 0;
    private int frameBodyLength = 0;
    private int total;
    private byte[] frameBody = new byte[0];
    private byte[] finalBytes = null;

    private static int connectNum = 30;
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

    public void iosMinicapSocketStart(Socket socket) {
        InputStream stream = null;
        DataInputStream input = null;
        try {
            if (connectNum <= 0 ) {
                return;
            }
            connectNum--;
            System.out.println("checking minicap waiting...:"+connectNum);
            socket = new Socket("127.0.0.1", 12345);
            System.out.println("exec:127.0.0.1:12345 socket 连接成功");
                while (true) {
                    stream = socket.getInputStream();
                    input = new DataInputStream(stream);
                    byte[] buffer;
                    int len = 0;
                    while (len == 0) {
                        len = input.available();
                    }
                    buffer = new byte[len];
                    input.read(buffer);
                    System.out.println("length=" + buffer.length);
                    if(buffer.length == 4){
                        System.out.println("content=" + buffer[0]);
                    }
                    byte[] currentBuffer = subByteArray(buffer, 0, buffer.length);
                    for (int cursor = 0; cursor < len;) {
                        int byte10 = buffer[cursor] & 0xff;
                        if (readFrameBytes < 4) {
                            // 第二次的缓冲区中前4位数字和为frame的缓冲区大小
                            frameBodyLength += (byte10 << (readFrameBytes * 8)) >>> 0;
                            cursor += 1;
                            readFrameBytes += 1;
                        } else {
                            if (len - cursor >= frameBodyLength && frameBodyLength > 0) {
                                if (frameBodyLength != 0) {
                                    byte[] subByte = subByteArray(currentBuffer, cursor, cursor + frameBodyLength);
                                    frameBody = byteMerger(frameBody, subByte);

                                    if ((frameBody[0] != -1) || frameBody[1] != -40) {
                                        System.out.println(String.format("Frame body does not start with JPG header"));
                                        String errorMsg = "画面传输异常，请关闭录制窗口重新执行关键字后继续录制";
                                        System.out.println(errorMsg);
                                        return;
                                    }
                                    System.out.println(String.format("实际图片的大小 : %d", frameBody.length));
                                    finalBytes = subByteArray(frameBody, 0, frameBody.length);
                                    getPNGToLocal(System.getProperty("user.dir"));
                                }
                            }
                            frameBodyLength = 0;
                            readFrameBytes = 0;
                            frameBody = new byte[0];
                            cursor = len;
                        }
                        connectNum = 30;
                    }
                }

        } catch (Exception e) {
//				e.printStackTrace();
            try {
                Thread.sleep(2000);
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            iosMinicapSocketStart(socket);
        }
    }

    private void createImageFromByte() throws IOException {
        if (finalBytes.length == 0) {
            System.out.println("frameBody大小为0");
        }
        InputStream in = new ByteArrayInputStream(finalBytes);
        BufferedImage bufferedImage = ImageIO.read(in);
//        notifyObservers(bufferedImage);
        String filePath = String.format("0.jpg");
        System.out.println(filePath);
        ImageIO.write(bufferedImage, "jpg", new File(filePath));
        finalBytes = null;
    }

    // java合并两个byte数组
    private static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    private static byte[] subByteArray(byte[] byte1, int start, int end) {
        byte[] byte2 = new byte[end - start];
        System.arraycopy(byte1, start, byte2, 0, end - start);
        return byte2;
    }

    /**
     * 关闭ios-minicap服务
     */
    public static void iosMinicapClose()
    {
        String path = System.getProperty("user.dir");
        String exitMinicapCmd = path+"/exitMinicap.sh ";
        try {
            Runtime.getRuntime().exec(exitMinicapCmd);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("Failed to close ios-minicap server.exitMinicapCmd=<" + exitMinicapCmd + ">");
        }
    }

    public void getPNGToLocal(String path) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] imgbyte = decoder.decodeBuffer();
        OutputStream os = new FileOutputStream(path + "/" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".png");
        os.write(finalBytes, 0, finalBytes.length);
        os.flush();
        os.close();
    }

    public static void main(String[] args) {
        IOSMinicapMachine socket = new IOSMinicapMachine();
        socket.iosMinicapStart();
    }
}

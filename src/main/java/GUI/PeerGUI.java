package GUI;

import WBSYS.CanvasShape;
import WBSYS.Properties;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static Service.Utils.shape2ProtoShape;
import static WBSYS.Properties.chatMessageFormat;
import static java.lang.System.err;

public class PeerGUI implements IClient, MouseListener, MouseMotionListener, ActionListener, WindowListener {
    private final String[] shapes = {"pen", "line", "circle", "oval", "rectangle", "eraser", "text"};
    private final String username;
    private final WhiteBoard wb;
    private JFrame peerFrame;
    private int portNumber;
    private String WBName;
    private JButton newFileButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton closeButton;
    private JColorChooser colorChooser;
    private String currentShapeType = "pen";
    private Color color = Color.BLACK;
    private int x1, x2, y1, y2;
    private JLabel IpLabel;
    private JLabel portLabel;
    private JButton kickButton;
    private ConcurrentLinkedDeque<Point2D> pointQ;
    private Graphics2D canvasGraphics;
    private boolean isFill = false;
    SettableFuture<Boolean> futurePreviewAccept;


    public PeerGUI(WhiteBoard whiteBoard, String username) {

        initComponents();
        this.wb = whiteBoard;
        this.username = username;
    }

    public void Build() {
        peerFrame = new JFrame();
        peerFrame.setBounds(150, 100, 1490, 800);
        peerFrame.setResizable(true);
        peerFrame.add(peerPanel);
        peerFrame.setResizable(false);
        peerPanel.setComponentZOrder(canvasPanel, 1);
        peerPanel.setComponentZOrder(chatPanel, 2);
        setColorChooser();
        setStrokeCB();
        setShapeButtons();
        setSendButton();
        setFillButton();
        chatTA.setAutoscrolls(true);
        sendTextField.setAutoscrolls(true);
        userTA.setAutoscrolls(true);

        canvasPanel.addMouseListener(this);
        canvasPanel.addMouseMotionListener(this);
        peerFrame.addWindowListener(this);
        peerFrame.pack();
        peerFrame.setVisible(true);

        canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
        System.out.println("peerGUI built. _______From PeerUI________");
    }

    private void setFileButtons() {

    }

    private void setColorChooser() {
        colorLabel.setOpaque(true);
        colorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                color = JColorChooser.showDialog(peerFrame, "choose color", null);
                if (color == null) {
                    return;
                }
                colorLabel.setBackground(color);
            }
        });
    }

    private void setStrokeCB() {
        strokeCB.addItem(1);
        strokeCB.addItem(2);
        strokeCB.addItem(3);
        strokeCB.addItem(5);
        strokeCB.addItem(7);
        strokeCB.addItem(9);
        strokeCB.addItem(11);
        strokeCB.addItem(26);
        strokeCB.addItem(25);
    }

    private void setSendButton() {
        SendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = sendTextField.getText();
                if (message != null) {

                    wb.pushMessage(chatMessageFormat(username, message));
                    sendTextField.setText(null);

                } else {
                    JOptionPane.showMessageDialog(peerFrame, "plz input your message to send");
                }
            }
        });
    }

    private void setFillButton() {
        fillButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isFill) {
                    isFill = false;
                    fillButton.setBackground(Color.WHITE);
                } else {
                    isFill = true;
                    fillButton.setBackground(Color.LIGHT_GRAY);
                }
            }
        });
    }

    public void drawCanvasShape(CanvasShape canvasShape) {
        SwingUtilities.invokeLater(() -> {
            Thread.ofVirtual().start(() -> {
                if(canvasGraphics == null){
                    canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
                }

                String shapeType = canvasShape.getShapeString();
                int x1 = canvasShape.getX1();
                int y1 = canvasShape.getY1();
                int x2 = canvasShape.getX2();
                int y2 = canvasShape.getY2();
                Color shapeColor = canvasShape.getColor();
                String username = canvasShape.getUsername();
                int strokeInt = canvasShape.getStrokeInt();
                Stroke stroke = new BasicStroke(strokeInt);

//                canvasGraphics = (Graphics2D) canvasPanel.getGraphics();

                canvasGraphics.setPaint(shapeColor);
                switch (shapeType) {
                    case "line" -> {
                        canvasGraphics.setStroke(stroke);
                        canvasGraphics.drawLine(x1, y1, x2, y2);
                    }
                    case "circle", "oval", "rectangle" -> {
                        canvasGraphics.setStroke(stroke);
                        int height = Math.abs(y2 - y1);
                        int width = Math.abs(x2 - x1);
                        if (canvasShape.isFill()) {
                            switch (shapeType) {
                                case "circle":
                                    canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height), Math.max(width, height));
                                    break;
                                case "oval":
                                    canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
                                    break;
                                case "rectangle":
                                    canvasGraphics.fillRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
                                    break;
                            }
                        } else {
                            switch (shapeType) {
                                case "circle":
                                    canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height), Math.max(width, height));
                                    break;
                                case "oval":
                                    canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
                                    break;
                                case "rectangle":
                                    canvasGraphics.drawRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
                                    break;
                            }
                        }
                    }
                    case "text" -> {
                        int size = canvasShape.getStrokeInt();
                        canvasGraphics.setFont(new Font("Times New Roman", Font.PLAIN, size * 2 + 10));
                        canvasGraphics.drawString(canvasShape.getText(), x1, y1);
                    }
                    case "pen", "eraser" -> {
                        ArrayList<Point2D> points = canvasShape.getPoints();
                        canvasGraphics.setStroke(stroke);
                        if (points != null && points.size() > 1) {
                            for (int i = 0; i < points.size() - 1; i++) {
                                int x3 = (int) points.get(i).getX();
                                int y3 = (int) points.get(i).getY();
                                int x4 = (int) points.get(i + 1).getX();
                                int y4 = (int) points.get(i + 1).getY();
                                canvasGraphics.setPaint(shapeColor);
                                canvasGraphics.drawLine(x3, y3, x4, y4);
                            }
                        }
                    }

//            canvasGraphics.notify();
                }
                //restore to chosen
                canvasGraphics.setStroke(new BasicStroke(Integer.parseInt(strokeCB.getSelectedItem().toString())));
            });
        });
    }

    private void setShapeButtons() {
        JButton[] buttons = {penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton};
        for (int i = 0; i < buttons.length; i++) {
            int index = i;
            buttons[i].addActionListener(e -> {
                chooseToolButton(index);
                currentShapeType = shapes[index];
            });
        }
    }

    private void chooseToolButton(int index) {
        JButton[] buttons = {penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBackground(i == index ? Color.lightGray : Color.WHITE);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(canvasGraphics == null){
            canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
        }
        wb.reportUpdEditing("add", username);

        x1 = e.getX();
        y1 = e.getY();
        if(pointQ == null){
            pointQ = new ConcurrentLinkedDeque<>();
        }else{
            pointQ.clear();
        }

        if (currentShapeType.equals("text")) {
            if(Properties.RepeatTextPreviewSending){
                new Thread(() -> {  // 创建新线程以避免UI阻塞
                    CanvasShape tmp = new CanvasShape(currentShapeType, Color.GRAY, x1 - 5, x1 + 5, y1 - 5, y1 + 5, Integer.parseInt(strokeCB.getSelectedItem().toString()));
                    tmp.setText("Other user is typing...");
                    if (wb.previewTmpStream == null) {
                        futurePreviewAccept = wb.sBeginPushShape();
                    }
                    for (int i = 0; i < 100; i++) {  // 循环发送10次
                        if (wb.previewTmpStream != null) {
                            wb.previewTmpStream.onNext(shape2ProtoShape(tmp));  // 发送消息
                        }
                        try {
                            Thread.sleep(20);  // 等待200毫秒
                        } catch (InterruptedException f) {
                            Thread.currentThread().interrupt();
                            System.err.println("Sending thread was interrupted.");
                            break;
                        }
                    }
                }).start();  // 启动线程
            }else{
                CanvasShape tmp = new CanvasShape(currentShapeType, Color.GRAY, x1 - 5, x1 + 5, y1 - 5, y1 + 5, Integer.parseInt(strokeCB.getSelectedItem().toString()));
                tmp.setText("Other user is typing...");
                if (wb.previewTmpStream == null) {
                    futurePreviewAccept = wb.sBeginPushShape();
                }
                if (wb.previewTmpStream != null) {
                    wb.previewTmpStream.onNext(shape2ProtoShape(tmp));  // 发送消息
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        wb.previewTmpStream.onCompleted();
        wb.previewTmpStream = null;

        Futures.addCallback(futurePreviewAccept, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                // 调用 SwingUtilities 确保 UI 更新在正确的线程执行
                SwingUtilities.invokeLater(() -> {
                    if (Boolean.TRUE.equals(result)) {
                        System.out.println("Preview accepted");
                        conductPushShape();
                    } else {
                        System.out.println("Preview rejected");
                        // 进行拒绝后的操作，如不 pushShape
                        reDraw();
                    }
                });
            }

            private void conductPushShape() {
                try {
                    if(futurePreviewAccept.get()){
                        System.out.println("preview accepted");
                        //如果被接受，就pushShape
                        wb.reportUpdEditing("remove", username);

                        x2 = e.getX();
                        y2 = e.getY();

                        int strokeInShape = Integer.parseInt(strokeCB.getSelectedItem().toString());

                        CanvasShape canvasShape;
                        if (currentShapeType.equals("pen") || currentShapeType.equals("eraser")) {
                            Color tempColor = color;
                            if (currentShapeType.equals("eraser")) {
                                tempColor = Color.white;
                            }
                            canvasShape = new CanvasShape(currentShapeType, tempColor, username, new ArrayList<>(pointQ), strokeInShape);
                        } else if (currentShapeType.equals("text")) {
                            canvasShape = new CanvasShape(currentShapeType, color, x1, x2, y1, y2, strokeInShape);
                            String texts = JOptionPane.showInputDialog(peerFrame, "input your text", "text", JOptionPane.PLAIN_MESSAGE, null, null, null).toString();
                            canvasShape.setText(texts);
                            canvasShape.setStrokeInt(Integer.parseInt(strokeCB.getSelectedItem().toString()));
                        } else {
                            //起终点可界定的图形
                            canvasShape = new CanvasShape(currentShapeType, color, x1, x2, y1, y2, strokeInShape);
                        }

                        canvasShape.setFill(isFill);
                        wb.getCanvasShapeArrayList().add(canvasShape);
                        wb.pushShape(canvasShape);
                        reDraw();//让pen落实到画布上
                    }else{
                        System.out.println("preview rejected");
                        //如果被拒绝，就不pushShape
                        reDraw();
                    }
                } catch (InterruptedException ex) {
                    err.println(ex);
                } catch (ExecutionException ex) {
                    err.println(ex);
                }
                futurePreviewAccept = null;
            }

            @Override
            public void onFailure(Throwable t) {
                // 处理错误
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Error during preview: " + t.getMessage());
                    // 可以在这里进行错误处理相关的 UI 更新
                });
            }
        }, Executors.newSingleThreadExecutor());

    }
    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    @Override
    public void mouseDragged(MouseEvent e) {
        CanvasShape tmp;
        canvasPanel.repaint();
        reDraw();
        int x3 = e.getX();
        int y3 = e.getY();
        int x4 = x1;
        int y4 = y1;
//        canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
        Stroke tempStroke = new BasicStroke(Integer.parseInt(strokeCB.getSelectedItem().toString()));
        if (currentShapeType.equals("pen") || currentShapeType.equals("eraser")) {
            if (!pointQ.isEmpty()) {
                x4 = (int) pointQ.getLast().getX();
                y4 = (int) pointQ.getLast().getY();
            }
            Color tempColor;
            if (currentShapeType.equals("eraser")) {
                tempColor = Color.white;
            }else {
                tempColor = color;
            }
            int finalX = x4, finalY = y4;
            SwingUtilities.invokeLater(() -> {
                canvasGraphics.setPaint(tempColor);
                canvasGraphics.setStroke(tempStroke);
                canvasGraphics.drawLine(finalX, finalY, x3, y3);
            });
            pointQ.add(new Point(x3, y3));

            tmp = new CanvasShape(currentShapeType, tempColor, username, new ArrayList<>(pointQ), Integer.parseInt(strokeCB.getSelectedItem().toString()));
            SwingUtilities.invokeLater(() -> drawCanvasShape(tmp));
//            wb.tempShapes.put(username, tmp);
        }else{
            tmp = new CanvasShape(currentShapeType, color, x1, x3, y1, y3, Integer.parseInt(strokeCB.getSelectedItem().toString()));
            SwingUtilities.invokeLater(() -> drawCanvasShape(tmp));
            wb.tempShapes.put(username, tmp);
        }


        if(wb.previewTmpStream == null){
            futurePreviewAccept = wb.sBeginPushShape();
        }else{
            wb.previewTmpStream.onNext(shape2ProtoShape(tmp));
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }


    @Override
    public void warningFromManager(String message) {
        JOptionPane.showMessageDialog(peerFrame, message);
    }

    @Override
    public void updateShapes(CanvasShape canvasShape) {
        new Thread() {
            @Override
            public void run() {
                drawCanvasShape(canvasShape);
            }
        }.start();
    }

    @Override
    public void updateChatBox(String chatMessage) {
        new Thread() {
            @Override
            public void run() {
                chatTA.append(chatMessage + "\n");
            }
        }.start();
    }

    @Override
    public boolean approveClientRequest(String username) {
        return false;
    }

    @Override
    public void updatePeerList(ArrayList<String> userlist) {
        new Thread() {
            @Override
            public void run() {
                userTA.setText(null);
                for (String user : userlist) {
                    userTA.append(user + "\n");
                }

            }
        }.start();
    }

    @Override
    public boolean requestFromPeer(String request) {
        return false;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void reDraw() {
        SwingUtilities.invokeLater(() -> {
            canvasPanel.repaint();
            new Thread(() -> {
                for (CanvasShape shape : wb.getTempShapes().values()) {
                    drawCanvasShape(shape);
                }
//            AtomicReference<ArrayList<CanvasShape>> shapeArrayList = new AtomicReference<>(wb.getCanvasShapeArrayList());
                for (CanvasShape shape : wb.getCanvasShapeArrayList()) {
                    drawCanvasShape(shape);
                }
            }).start();
        });
    }


    @Override
    public void closeWindow(String message) {
        Thread.ofVirtual().start((() -> {
            JOptionPane.showMessageDialog(peerFrame, message.isEmpty() ? "Your whiteboard is closing involuntarily" : message);
            peerFrame.setVisible(false);
            System.exit(0);
        }));
    }

    @Override
    public void exit() {
        peerFrame.setVisible(false);
    }

    @Override
    public void clearCanvas() {
        wb.getCanvasShapeArrayList().clear();
        canvasPanel.repaint();
    }

    @Override
    public void showEditing() {
        new Thread() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                for(String str : wb.getEditingUser()){
                    stringBuilder.append(str).append(",");
                }
                if(stringBuilder.isEmpty()){
                    stringBuilder.append("Nobody");
                }else {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                stringBuilder.append(" is editing.");
                editingJLabel.setText(stringBuilder.toString());
            }
        }.start();
    }
    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        try {
            wb.peerExit(this.username);
        } finally {
            e.getWindow().dispose();
        }

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {
        reDraw();
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        reDraw();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        reDraw();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        reDraw();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Evaluation license - yongchun
        var panel1 = new JPanel();
        peerPanel = new JPanel();
        canvasPanel = new JPanel();
        chatPanel = new JPanel();
        usersSP = new JScrollPane();
        userTA = new JTextArea();
        ChatSP = new JScrollPane();
        chatTA = new JTextArea();
        settingBar = new JToolBar();
        shapeColorBar = new JToolBar();
        colorButton = new JButton();
        colorLabel = new JLabel();
        penButton = new JButton();
        lineButton = new JButton();
        circleButton = new JButton();
        ovalButton = new JButton();
        rectButton = new JButton();
        earserButton = new JButton();
        textButton = new JButton();
        fillButton = new JButton();
        var label1 = new JLabel();
        strokeCB = new JComboBox();
        editingJLabel = new JLabel();
        SendButton = new JButton();
        sendTextField = new JTextField();

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setBorder ( new javax . swing. border .CompoundBorder ( new javax . swing. border .TitledBorder ( new
            javax . swing. border .EmptyBorder ( 0, 0 ,0 , 0) ,  "JFor\u006dDesi\u0067ner \u0045valu\u0061tion" , javax
            . swing .border . TitledBorder. CENTER ,javax . swing. border .TitledBorder . BOTTOM, new java
            . awt .Font ( "Dia\u006cog", java .awt . Font. BOLD ,12 ) ,java . awt
            . Color .red ) ,panel1. getBorder () ) ); panel1. addPropertyChangeListener( new java. beans .
            PropertyChangeListener ( ){ @Override public void propertyChange (java . beans. PropertyChangeEvent e) { if( "bord\u0065r" .
            equals ( e. getPropertyName () ) )throw new RuntimeException( ) ;} } );
            panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));

            //======== peerPanel ========
            {
                peerPanel.setMaximumSize(new Dimension(1490, 850));
                peerPanel.setMinimumSize(new Dimension(1490, 850));
                peerPanel.setPreferredSize(new Dimension(1490, 850));
                peerPanel.setName("peerPanel");
                peerPanel.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));

                //======== canvasPanel ========
                {
                    canvasPanel.setBackground(Color.white);
                    canvasPanel.setBorder(new TitledBorder(new EtchedBorder(), "Canvas area", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                        UIManager.getFont("TitledBorder.font").deriveFont(14f)));
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    canvasPanel.setName("canvasPanel");
                    canvasPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
                }
                peerPanel.add(canvasPanel, new GridConstraints(2, 0, 2, 1,
                    GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_CAN_GROW,
                    new Dimension(1130, 700), new Dimension(1130, 700), new Dimension(1130, 700)));

                //======== chatPanel ========
                {
                    chatPanel.setInheritsPopupMenu(false);
                    chatPanel.setName("chatPanel");
                    chatPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));

                    //======== usersSP ========
                    {
                        usersSP.setBorder(new TitledBorder(null, "Users connecting", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                            UIManager.getFont("TitledBorder.font").deriveFont(14f), Color.black));
                        usersSP.setName("usersSP");

                        //---- userTA ----
                        userTA.setEditable(false);
                        userTA.setMargin(new Insets(10, 10, 10, 10));
                        userTA.setFocusable(false);
                        userTA.setName("userTA");
                        usersSP.setViewportView(userTA);
                    }
                    chatPanel.add(usersSP, new GridConstraints(0, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        new Dimension(350, 220), new Dimension(350, 220), new Dimension(350, 220)));

                    //======== ChatSP ========
                    {
                        ChatSP.setBorder(new TitledBorder(null, "Chat area", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                            UIManager.getFont("TitledBorder.font").deriveFont(14f)));
                        ChatSP.setName("ChatSP");

                        //---- chatTA ----
                        chatTA.setEditable(false);
                        chatTA.setMaximumSize(new Dimension(350, 460));
                        chatTA.setMinimumSize(new Dimension(350, 460));
                        chatTA.setPreferredSize(new Dimension(350, 460));
                        chatTA.setName("chatTA");
                        ChatSP.setViewportView(chatTA);
                    }
                    chatPanel.add(ChatSP, new GridConstraints(1, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        new Dimension(350, 460), new Dimension(350, 460), null));
                }
                peerPanel.add(chatPanel, new GridConstraints(0, 1, 3, 1,
                    GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    new Dimension(350, 700), new Dimension(350, 700), new Dimension(350, 700)));

                //======== settingBar ========
                {
                    settingBar.setBackground(Color.white);
                    settingBar.setEnabled(true);
                    settingBar.setFloatable(false);
                    settingBar.setMargin(new Insets(5, 5, 5, 5));
                    settingBar.setBorder(new TitledBorder(LineBorder.createBlackLineBorder(), ""));
                    settingBar.setName("settingBar");
                }
                peerPanel.add(settingBar, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(1130, 30), new Dimension(1130, 30), new Dimension(1130, 30)));

                //======== shapeColorBar ========
                {
                    shapeColorBar.setBackground(Color.white);
                    shapeColorBar.setFloatable(false);
                    shapeColorBar.setMargin(new Insets(5, 5, 5, 5));
                    shapeColorBar.setBorder(new TitledBorder(LineBorder.createBlackLineBorder(), ""));
                    shapeColorBar.setName("shapeColorBar");

                    //---- colorButton ----
                    colorButton.setMaximumSize(new Dimension(108, 40));
                    colorButton.setMinimumSize(new Dimension(108, 40));
                    colorButton.setPreferredSize(new Dimension(108, 40));
                    colorButton.setText("choose Color");
                    colorButton.setName("colorButton");
                    shapeColorBar.add(colorButton);

                    //---- colorLabel ----
                    colorLabel.setAutoscrolls(false);
                    colorLabel.setBackground(Color.black);
                    colorLabel.setMaximumSize(new Dimension(40, 40));
                    colorLabel.setMinimumSize(new Dimension(40, 40));
                    colorLabel.setOpaque(true);
                    colorLabel.setPreferredSize(new Dimension(40, 40));
                    colorLabel.setText("");
                    colorLabel.setName("colorLabel");
                    shapeColorBar.add(colorLabel);

                    //---- penButton ----
                    penButton.setText("pen");
                    penButton.setName("penButton");
                    shapeColorBar.add(penButton);

                    //---- lineButton ----
                    lineButton.setText("line");
                    lineButton.setName("lineButton");
                    shapeColorBar.add(lineButton);

                    //---- circleButton ----
                    circleButton.setText("circle");
                    circleButton.setName("circleButton");
                    shapeColorBar.add(circleButton);

                    //---- ovalButton ----
                    ovalButton.setText("oval");
                    ovalButton.setName("ovalButton");
                    shapeColorBar.add(ovalButton);

                    //---- rectButton ----
                    rectButton.setText("rect");
                    rectButton.setName("rectButton");
                    shapeColorBar.add(rectButton);

                    //---- earserButton ----
                    earserButton.setText("earser");
                    earserButton.setName("earserButton");
                    shapeColorBar.add(earserButton);

                    //---- textButton ----
                    textButton.setText("text");
                    textButton.setName("textButton");
                    shapeColorBar.add(textButton);

                    //---- fillButton ----
                    fillButton.setText("Fill");
                    fillButton.setName("fillButton");
                    shapeColorBar.add(fillButton);

                    //---- label1 ----
                    label1.setText("Stroke");
                    label1.setName("label1");
                    shapeColorBar.add(label1);

                    //---- strokeCB ----
                    strokeCB.setMaximumSize(new Dimension(78, 30));
                    strokeCB.setMinimumSize(new Dimension(78, 30));
                    strokeCB.setToolTipText("stroke");
                    strokeCB.setName("strokeCB");
                    shapeColorBar.add(strokeCB);

                    //---- editingJLabel ----
                    editingJLabel.setText("");
                    editingJLabel.setName("editingJLabel");
                    shapeColorBar.add(editingJLabel);
                }
                peerPanel.add(shapeColorBar, new GridConstraints(1, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(1130, 70), new Dimension(1130, 70), new Dimension(1130, 70)));

                //---- SendButton ----
                SendButton.setText("Send");
                SendButton.setName("SendButton");
                peerPanel.add(SendButton, new GridConstraints(4, 1, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null, null, null));

                //---- sendTextField ----
                sendTextField.setName("sendTextField");
                peerPanel.add(sendTextField, new GridConstraints(3, 1, 1, 1,
                    GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(350, 100), new Dimension(350, 100), new Dimension(350, 100)));
            }
            panel1.add(peerPanel, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Evaluation license - yongchun
    private JPanel peerPanel;
    private JPanel canvasPanel;
    private JPanel chatPanel;
    private JScrollPane usersSP;
    private JTextArea userTA;
    private JScrollPane ChatSP;
    private JTextArea chatTA;
    private JToolBar settingBar;
    private JToolBar shapeColorBar;
    private JButton colorButton;
    private JLabel colorLabel;
    private JButton penButton;
    private JButton lineButton;
    private JButton circleButton;
    private JButton ovalButton;
    private JButton rectButton;
    private JButton earserButton;
    private JButton textButton;
    private JButton fillButton;
    private JComboBox strokeCB;
    private JLabel editingJLabel;
    private JButton SendButton;
    private JTextField sendTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
